/**
 * description :
 * E-mail:csqwyyx@163.com
 * github:https://github.com/John-Chen
 */
package com.csq.downloadmanager.util;

import android.net.Uri;
import android.webkit.MimeTypeMap;

import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlUtil {

    /**
     * Guesses canonical filename that a download would have, using
     * the URL and contentDisposition. File extension, if not defined,
     * is added based on the mimetype
     * @param url Url to the content
     * @param contentDisposition Content-Disposition HTTP header or null
     * @param mimeType Mime-type of the content or null
     *
     * @return suggested filename
     */
    public static final String guessFileName(
            String url,
            String contentDisposition,
            String mimeType) {
        String filename = null;
        String extension = null;

        // If we couldn't do anything with the hint, move toward the content disposition
        if (filename == null && contentDisposition != null) {
            filename = parseFileNameFromContentDisposition(contentDisposition);
            if (filename != null) {
                int index = filename.lastIndexOf('/') + 1;
                if (index > 0) {
                    filename = filename.substring(index);
                }
            }
        }

        // If all the other http-related approaches failed, use the plain uri
        if (filename == null) {
            String decodedUrl = Uri.decode(url);
            if (decodedUrl != null) {
                int queryIndex = decodedUrl.indexOf('?');
                // If there is a query string strip it, same as desktop browsers
                if (queryIndex > 0) {
                    decodedUrl = decodedUrl.substring(0, queryIndex);
                }
                if (!decodedUrl.endsWith("/")) {
                    int index = decodedUrl.lastIndexOf('/') + 1;
                    if (index > 0) {
                        filename = decodedUrl.substring(index);
                    }
                }
            }
        }

        // Finally, if couldn't get filename from URI, get a generic filename
        if (filename == null) {
            filename = "downloadfile";
        }

        // Split filename between base and extension
        // Add an extension if filename does not have one
        int dotIndex = filename.indexOf('.');
        if (dotIndex < 0) {
            if (mimeType != null) {
                extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
                if (extension != null) {
                    extension = "." + extension;
                }
            }
            if (extension == null) {
                if (mimeType != null && mimeType.toLowerCase(java.util.Locale.ROOT).startsWith("text/")) {
                    if (mimeType.equalsIgnoreCase("text/html")) {
                        extension = ".html";
                    } else {
                        extension = ".txt";
                    }
                } else {
                    extension = ".bin";
                }
            }
        } else {
            if (mimeType != null) {
                // Compare the last segment of the extension against the mime type.
                // If there's a mismatch, discard the entire extension.
                int lastDotIndex = filename.lastIndexOf('.');
                String typeFromExt = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        filename.substring(lastDotIndex + 1));
                if (typeFromExt != null && !typeFromExt.equalsIgnoreCase(mimeType)) {
                    extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
                    if (extension != null) {
                        extension = "." + extension;
                    }
                }
            }
            if (extension == null) {
                extension = filename.substring(dotIndex);
            }
            filename = filename.substring(0, dotIndex);
        }

        return filename + extension;
    }

    private static final Pattern CONTENT_DISPOSITION_RFC_PATTERN =
            Pattern.compile("attachment;\\s*filename\\s*.*=\\s*([^\"]*)''([^\"]*)",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTENT_DISPOSITION_PATTERN =
            Pattern.compile("attachment;\\s*filename\\s*=\\s*(\"?)([^\"]*)\\1\\s*$",
                    Pattern.CASE_INSENSITIVE);

    /**
     * 解析Content-disposition里面对应的filename
     * @param contentDisposition
     * attachment;filename*=UTF-8''Java%20%E5%B9%B6%E5%8F%91%E7%BC%96%E7%A8%8B%E7%9A%84%E8%89%BA%E6%9C%AF.pdf
     * or
     * attachment; filename="°®.mp3"
     * @return filename
     */
    static String parseFileNameFromContentDisposition(String contentDisposition) {
        try {
            Matcher m = CONTENT_DISPOSITION_RFC_PATTERN.matcher(contentDisposition);
            if (m.find()) {
                try {
                    return URLDecoder.decode(m.group(2), m.group(1));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            m = CONTENT_DISPOSITION_PATTERN.matcher(contentDisposition);
            if (m.find()) {
                String g = new String(m.group(2).getBytes("ISO-8859-1"), "GBK");  //IDEA测试没问题？？？
                //String iso = new String(g.getBytes("UTF-8"),"ISO-8859-1");
                //String utf8 = new String(iso.getBytes("ISO-8859-1"),"UTF-8");
                return g.toString();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

}
