package com.liferay.portal.site.initializer.internal.service.ddmstructure.helper;

import com.liferay.document.library.kernel.model.DLFileEntryTypeConstants;
import com.liferay.dynamic.data.mapping.constants.DDMStructureConstants;
import com.liferay.dynamic.data.mapping.constants.DDMTemplateConstants;
import com.liferay.dynamic.data.mapping.io.*;
import com.liferay.dynamic.data.mapping.model.*;
import com.liferay.dynamic.data.mapping.service.DDMStructureLocalService;
import com.liferay.dynamic.data.mapping.service.DDMTemplateLocalService;
import com.liferay.dynamic.data.mapping.storage.StorageType;
import com.liferay.dynamic.data.mapping.util.DDM;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.template.TemplateConstants;
import com.liferay.portal.kernel.upgrade.util.UpgradeProcessUtil;
import com.liferay.portal.kernel.util.*;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.UnsecureSAXReaderUtil;
import com.liferay.portal.site.initializer.extender.context.InitializerContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.ServletContext;
import java.util.*;

@Component(service = DDMStructureHelper.class)
public class DDMStructureHelperImpl implements DDMStructureHelper {

    @Override
    public void addDDMStructures(long classNameId, String fileName, InitializerContext context) throws Exception {

        ServiceContext serviceContext = context.getServiceContext();
        long userId = serviceContext.getUserId();
        long groupId = serviceContext.getScopeGroupId();

        Locale locale = _portal.getSiteDefaultLocale(groupId);
        ServletContext servletContext = context.getServletContext();
        ClassLoader classLoader = servletContext.getClassLoader();

        List<Element> structureElements = _getDDMStructures(classLoader, fileName, locale);

        for (Element structureElement : structureElements) {
            boolean dynamicStructure = GetterUtil.getBoolean(structureElement.elementText("dynamic-structure"));

            if (dynamicStructure) {
                continue;
            }

            String name = structureElement.elementText("name");

            String ddmStructureKey = name;

            DDMStructure ddmStructure = _ddmStructureLocalService.fetchStructure(groupId, classNameId, ddmStructureKey);

            //todo: check this logic
            if ((ddmStructure != null) || (name.equals(DLFileEntryTypeConstants.NAME_IG_IMAGE) && !UpgradeProcessUtil.isCreateIGImageDocumentType())) {
                continue;
            }

            String description = structureElement.elementText("description");

            Map<Locale, String> nameMap = new HashMap<>();
            Map<Locale, String> descriptionMap = new HashMap<>();

            for (Locale curLocale : _language.getAvailableLocales(groupId)) {
                ResourceBundle resourceBundle =ResourceBundleUtil.getModuleAndPortalResourceBundle(curLocale, getClass());
                nameMap.put(curLocale, _language.get(resourceBundle, name));
                descriptionMap.put(curLocale, _language.get(resourceBundle, description));
            }

            DDMForm ddmForm = getDDMForm(groupId, locale, structureElement);

            DDMFormLayout ddmFormLayout = _getDDMFormLayout(structureElement, ddmForm);

            serviceContext.setAttribute("status", WorkflowConstants.STATUS_APPROVED);

            ddmStructure = _ddmStructureLocalService.addStructure(
                    userId, groupId,
                    com.liferay.dynamic.data.mapping.constants.DDMStructureConstants.DEFAULT_PARENT_STRUCTURE_ID, classNameId,
                    ddmStructureKey, nameMap, descriptionMap, ddmForm,
                    ddmFormLayout, StorageType.DEFAULT.toString(),
                    DDMStructureConstants.TYPE_DEFAULT, serviceContext);

            Element templateElement = structureElement.element("template");

            if (templateElement == null) {
                continue;
            }

            String templateFileName = templateElement.elementText("file-name");

            String script = StringUtil.read(classLoader, FileUtil.getPath(fileName) + StringPool.SLASH + templateFileName);

            boolean cacheable = GetterUtil.getBoolean(templateElement.elementText("cacheable"));

            _ddmTemplateLocalService.addTemplate(
                    userId, groupId, _portal.getClassNameId(DDMStructure.class),
                    ddmStructure.getStructureId(), ddmStructure.getClassNameId(),
                    name, nameMap, null, com.liferay.dynamic.data.mapping.constants.DDMTemplateConstants.TEMPLATE_TYPE_DISPLAY,
                    DDMTemplateConstants.TEMPLATE_MODE_CREATE,
                    TemplateConstants.LANG_TYPE_FTL, script, cacheable, false,
                    StringPool.BLANK, null, serviceContext);
        }

    }

    protected DDMForm getDDMForm(long groupId, Locale locale, Element structureElement) {
        Element structureElementDefinitionElement = structureElement.element("definition");
        if (structureElementDefinitionElement != null) {
            return deserialize(structureElementDefinitionElement.getTextTrim(), _jsonDDMFormDeserializer);
        }
        Element structureElementRootElement = structureElement.element("root");
        String definition = structureElementRootElement.asXML();
        DDMForm ddmForm = deserialize(definition, _xsdDDMFormDeserializer);
        ddmForm = _ddm.updateDDMFormDefaultLocale(ddmForm, locale);
        return _getPopulateDDMForm(ddmForm, locale, _language.getAvailableLocales(groupId));
    }

    private DDMFormLayout _getDDMFormLayout(Element structureElement, DDMForm ddmForm) {
        Element structureElementLayoutElement = structureElement.element("layout");
        if (structureElementLayoutElement != null) {
            DDMFormLayoutDeserializerDeserializeRequest.Builder builder = DDMFormLayoutDeserializerDeserializeRequest.Builder.newBuilder(structureElementLayoutElement.getTextTrim());
            DDMFormLayoutDeserializerDeserializeResponse ddmFormLayoutDeserializerDeserializeResponse = _jsonDDMFormLayoutDeserializer.deserialize(builder.build());
            return ddmFormLayoutDeserializerDeserializeResponse.getDDMFormLayout();
        }
        return _ddm.getDefaultDDMFormLayout(ddmForm);
    }

    private DDMForm _getPopulateDDMForm(DDMForm ddmForm, Locale defaultLocale, Set<Locale> locales) {
        for (Locale locale : locales) {
            ddmForm.addAvailableLocale(locale);
        }
        ddmForm.setDDMFormFields( _getPopulateDDMFormFields(ddmForm.getDDMFormFields(), defaultLocale, locales));
        return ddmForm;
    }

    private List<DDMFormField> _getPopulateDDMFormFields(List<DDMFormField> ddmFormFields, Locale defaultLocale, Set<Locale> locales) {
        for (DDMFormField ddmFormField : ddmFormFields) {
            DDMFormFieldOptions ddmFormFieldOptions = ddmFormField.getDDMFormFieldOptions();
            Map<String, LocalizedValue> options = ddmFormFieldOptions.getOptions();
            for (Map.Entry<String, LocalizedValue> entry : options.entrySet()) {
                options.put(entry.getKey(), _getPopulateLocalizedValue(defaultLocale, locales, entry.getValue()));
            }
            ddmFormField.setDDMFormFieldOptions(ddmFormFieldOptions);
            ddmFormField.setLabel(_getPopulateLocalizedValue(defaultLocale, locales, ddmFormField.getLabel()));
            ddmFormField.setNestedDDMFormFields(_getPopulateDDMFormFields(ddmFormField.getNestedDDMFormFields(), defaultLocale, locales));
            ddmFormField.setTip(_getPopulateLocalizedValue(defaultLocale, locales, ddmFormField.getTip()));
        }
        return ddmFormFields;
    }

    private LocalizedValue _getPopulateLocalizedValue(Locale defaultLocale, Set<Locale> locales, LocalizedValue localizedValue) {
        String defaultValue = localizedValue.getString(defaultLocale);
        for (Locale locale : locales) {
            ResourceBundle resourceBundle = ResourceBundleUtil.getModuleAndPortalResourceBundle(locale, getClass());
            localizedValue.addString(locale, _language.get(resourceBundle, defaultValue));
        }
        return localizedValue;
    }


    protected DDMForm deserialize(String content, DDMFormDeserializer ddmFormDeserializer) {
        DDMFormDeserializerDeserializeRequest.Builder builder = DDMFormDeserializerDeserializeRequest.Builder.newBuilder(content);
        DDMFormDeserializerDeserializeResponse ddmFormDeserializerDeserializeResponse = ddmFormDeserializer.deserialize(builder.build());
        return ddmFormDeserializerDeserializeResponse.getDDMForm();
    }

    private List<Element> _getDDMStructures(ClassLoader classLoader, String fileName, Locale locale) throws Exception {
        String xml = StringUtil.read(classLoader, fileName);
        xml = StringUtil.replace(xml, "[$LOCALE_DEFAULT$]", locale.toString());
        Document document = UnsecureSAXReaderUtil.read(xml);
        Element rootElement = document.getRootElement();
        return rootElement.elements("structure");
    }


    @Reference
    private DDM _ddm;
    @Reference
    private Portal _portal;
    @Reference
    private Language _language;

    @Reference(target = "(ddm.form.deserializer.type=xsd)")
    private DDMFormDeserializer _xsdDDMFormDeserializer;
    @Reference(target = "(ddm.form.deserializer.type=json)")
    private DDMFormDeserializer _jsonDDMFormDeserializer;
    @Reference(target = "(ddm.form.layout.deserializer.type=json)")
    private DDMFormLayoutDeserializer _jsonDDMFormLayoutDeserializer;

    @Reference
    private DDMTemplateLocalService _ddmTemplateLocalService;
    @Reference
    private DDMStructureLocalService _ddmStructureLocalService;

}