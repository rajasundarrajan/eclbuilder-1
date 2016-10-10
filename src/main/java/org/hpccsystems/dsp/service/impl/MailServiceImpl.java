package org.hpccsystems.dsp.service.impl;

import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.hpcc.HIPIE.Composition;
import org.hpcc.HIPIE.utils.PropertyManager;
import org.hpccsystems.dsp.service.MailService;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.zkoss.util.resource.Labels;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

@Service("mailService")
@Scope(value = "singleton", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class MailServiceImpl implements MailService {

    @Override
    public void notifyReadyforTesting(List<Composition> compositions, List<String> recipients) throws MessagingException {
        StringBuilder body = new StringBuilder()
                .append(Labels.getLabel("readyToTestBody"));
        
        compositions.forEach(composition -> body
                                            .append("<br/>")
                                            .append(composition.getCanonicalName()));
        
        sendMail(recipients, body.toString(), Labels.getLabel("readyToTestSubject"));
    }
    
    private static void sendMail(List<String> recipients, String body, String subject) 
            throws MessagingException {
        String smtpProps = PropertyManager.GetProperty("SMTP_CONNECTIONS");
        JSONObject json = (JSONObject) JSONValue.parse(smtpProps);
        
        String from = (String) json.get("senderAddr");
        String host = (String) json.get("host");
        
        Properties properties = System.getProperties();
        properties.setProperty("mail.smtp.host", host);
        Session session = Session.getDefaultInstance(properties);
        
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        Address[] recipientAddress = new Address[recipients.size()];
        int count = 0;
        for (String recipient : recipients) {
            recipientAddress[count] = new InternetAddress(recipient);
            count++;
        }
        
        message.addRecipients(Message.RecipientType.TO, recipientAddress);
        message.setSubject(subject);
        message.setContent(body, "text/html");
        Transport.send(message);
    }
    
}
