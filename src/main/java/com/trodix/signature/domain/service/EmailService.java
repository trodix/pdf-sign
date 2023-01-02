package com.trodix.signature.domain.service;

import java.io.IOException;
import java.util.Map;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import freemarker.template.Template;
import freemarker.template.TemplateException;

@Service
public class EmailService {

    private final JavaMailSender emailSender;

    private final FreeMarkerConfigurer freemarkerConfigurer;

    public EmailService(final JavaMailSender emailSender, final FreeMarkerConfigurer freemarkerConfigurer) {
        this.emailSender = emailSender;
        this.freemarkerConfigurer = freemarkerConfigurer;
    }

    private void sendHtmlMessage(final String to, final String subject, final String htmlBody) throws MessagingException {

        final MimeMessage message = emailSender.createMimeMessage();
        final MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom("noreply@trodix.com");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);

        emailSender.send(message);
    }

    public void sendNewSignTaskEmailNotification(String to, String subject, Map<String, Object> templateModel) throws IOException, TemplateException, MessagingException {

        final Template freemarkerTemplate = freemarkerConfigurer.getConfiguration().getTemplate("sign-task-notification.html.ftl");
        final String htmlBody = FreeMarkerTemplateUtils.processTemplateIntoString(freemarkerTemplate, templateModel);

        sendHtmlMessage(to, subject, htmlBody);

    }

    public void sendSignedDocumentEmailNotification(final String to, final String subject, final Map<String, Object> templateModel)
            throws IOException, TemplateException, MessagingException {

        final Template freemarkerTemplate = freemarkerConfigurer.getConfiguration().getTemplate("signed-document-notification.html.ftl");
        final String htmlBody = FreeMarkerTemplateUtils.processTemplateIntoString(freemarkerTemplate, templateModel);

        sendHtmlMessage(to, subject, htmlBody);
    }

    public void sendDownloadedDocumentEmailNotification(String to, String subject, Map<String, Object> templateModel) throws IOException, TemplateException, MessagingException {

        final Template freemarkerTemplate = freemarkerConfigurer.getConfiguration().getTemplate("downloaded-notification.html.ftl");
        final String htmlBody = FreeMarkerTemplateUtils.processTemplateIntoString(freemarkerTemplate, templateModel);

        sendHtmlMessage(to, subject, htmlBody);

    }

}
