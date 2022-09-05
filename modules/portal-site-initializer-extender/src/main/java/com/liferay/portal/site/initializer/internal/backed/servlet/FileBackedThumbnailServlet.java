package com.liferay.portal.site.initializer.internal.backed.servlet;

import com.liferay.portal.site.initializer.extender.PortalInitializerExtender;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

/**
 * @author Brian Wing Shun Chan
 */
@Component(
	immediate = true,
	property = {
		"osgi.http.whiteboard.servlet.name=com.liferay.portal.site.initializer.internal.backed.servlet.FileBackedThumbnailServlet",
		"osgi.http.whiteboard.servlet.pattern=/file-backed-portal-initializer/*",
		"servlet.init.httpMethods=GET"
	},
	service = Servlet.class
)
public class FileBackedThumbnailServlet extends HttpServlet {

	@Override
	protected void doGet(
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse)
		throws IOException {

		String pathInfo = httpServletRequest.getPathInfo();

		if ((pathInfo == null) || (pathInfo.length() <= 1)) {
			return;
		}

		pathInfo = pathInfo.substring(1);

		int index = pathInfo.indexOf("/");

		if (index == -1) {
			return;
		}

		String fileKey = pathInfo.substring(0, index);

		File file = _siteInitializerExtender.getFile(fileKey);

		if (file == null) {
			return;
		}

		file = new File(file, "thumbnail.png");

		httpServletResponse.setContentLength((int)file.length());

		httpServletResponse.setContentType("image/png");

		try (InputStream inputStream = new FileInputStream(file);
			OutputStream outputStream = httpServletResponse.getOutputStream()) {

			byte[] buffer = new byte[8192];

			int length = 0;

			while ((length = inputStream.read(buffer)) >= 0) {
				outputStream.write(buffer, 0, length);
			}
		}
	}

	@Reference
	private PortalInitializerExtender _siteInitializerExtender;

}