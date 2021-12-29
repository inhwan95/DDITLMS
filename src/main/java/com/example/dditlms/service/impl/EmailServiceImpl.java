package com.example.dditlms.service.impl;

import com.example.dditlms.domain.dto.EmailDTO;
import com.example.dditlms.domain.entity.Member;
import com.example.dditlms.security.AccountContext;
import com.example.dditlms.service.EmailService;
import com.example.dditlms.util.base64Parser;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import com.sun.mail.pop3.POP3Store;
import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import lombok.extern.slf4j.Slf4j;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Transactional(readOnly = true)
@Service
public class EmailServiceImpl implements EmailService {

    @Override
    public List<EmailDTO> receiveEmailList(String folderName) {

        //현재 로그인한 사용자 정보(userNumber)를 가져옴
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Member member = null;
        try {
            member = ((AccountContext) authentication.getPrincipal()).getMember();
        } catch (ClassCastException e) {
        }
        Long userNumber = member.getUserNumber();
        String domain = "@ddit.site";

        String imapHost = "mail.ddit.site";
        String storeType = "imap";
        String user = (member.getMemberId() + domain);
        String password = "java";


        try {
            //1) 세션값 받아옴
            Properties properties = new Properties();
            properties.put("mail.host", imapHost);
            Session emailSession = Session.getDefaultInstance(properties);

            //2) imap서버로 연결 함.
            IMAPStore emailStore = (IMAPStore) emailSession.getStore(storeType);
            emailStore.connect(user, password);


            //3) 받은 메일함에 폴더에 접근 한다.
            Folder[] folders = emailStore.getDefaultFolder().list("*");
            log.info("-----------------" + Arrays.toString(folders));
            Folder emailFolder = emailStore.getFolder(folderName);
            UIDFolder uf = (UIDFolder) emailFolder;
            emailFolder.open(Folder.READ_WRITE);


            //4) 받은 메세지 정보를 순차별로 담고, 반환한다.


            List<Message> messages = Arrays.asList(emailFolder.getMessages());
            List<EmailDTO> emailDTOList = new ArrayList<>();


            for (int i = 0; i < messages.size(); i++) {
                EmailDTO emailDTO = new EmailDTO();

                String convertAddress = base64Parser.getFileName(messages.get(i).getFrom()[0].toString());
                InternetAddress internetAddress = new InternetAddress();
                internetAddress.setAddress(convertAddress);
                emailDTO.setFromAddress(internetAddress);

                emailDTO.setContent(messages.get(i).getContent());
                emailDTO.setSubject(messages.get(i).getSubject());
                Address[] toList = messages.get(i).getRecipients(MimeMessage.RecipientType.TO);
                emailDTO.setToList(toList);
                Address[] ccList = messages.get(i).getRecipients(MimeMessage.RecipientType.CC);
                emailDTO.setCcList(ccList);
                //Date 타입을 LocalDate타입으로 변환(타임리프에서 편하게 사용하기 위함)
                LocalDateTime localDateTime = new Timestamp(messages.get(i).getSentDate().getTime()).toLocalDateTime();
                emailDTO.setSentDate(localDateTime);
                emailDTO.setMessageNumber(messages.get(i).getMessageNumber());
                emailDTO.setMailUID(uf.getUID(messages.get(i)));


                String contentType = messages.get(i).getContentType();
                String messageContent = "";

                // store attachment file name, separated by comma
                String attachFiles = "";
                String saveDirectory = "C:/temp/mail/";
                if (contentType.contains("multipart")) {
                    // content may contain attachments
                    Multipart multiPart = (Multipart) messages.get(i).getContent();
                    int numberOfParts = multiPart.getCount();
                    for (int partCount = 0; partCount < numberOfParts; partCount++) {
                        MimeBodyPart part = (MimeBodyPart) multiPart.getBodyPart(partCount);

                        if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                            // this part is attachment
                            String fileName = part.getFileName();
                            attachFiles += fileName + ", ";

                            part.saveFile(saveDirectory + File.separator + base64Parser.getFileName(fileName));
                        } else {
                            // this part may be the message content
                            messageContent = part.getContent().toString();
                        }
                    }
                    if (attachFiles.length() > 1) {
                        attachFiles = attachFiles.substring(0, attachFiles.length() - 2);
                    }

                } else if (contentType.contains("text/plain")
                        || contentType.contains("text/html")) {
                    Object content = messages.get(i).getContent();
                    if (content != null) {
                        messageContent = content.toString();
                    }
                }
                emailDTO.setContent(messageContent);
                emailDTO.setMailBox(folderName);
                emailDTOList.add(emailDTO);
            }

            //5) 사용한 자원을 반환.
            emailFolder.close(false);
            emailStore.close();
            return emailDTOList;

        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Base64DecodingException e) {
            e.printStackTrace();
        }

        return null;
    }

    // 이메일 상세 읽기
    @Override
    public EmailDTO viewMail(int messageNumber, List<EmailDTO> emailDTOList) {
        EmailDTO email = emailDTOList.get(messageNumber);

        return email;
    }

    // 이메일 쓰기
    @Override
    public void writeMail(EmailDTO emailDTO) throws IOException {

        //현재 로그인한 사용자 정보(userNumber)를 가져옴
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Member member = null;
        try {
            member = ((AccountContext) authentication.getPrincipal()).getMember();
        } catch (ClassCastException e) {

        }

        String domain = "@ddit.site";
        String fromName = member.getName();
        String fromAddress = (member.getMemberId() + domain);

        String subject = emailDTO.getSubject();
        Object content = emailDTO.getContent();
        String toAddress = emailDTO.getToAddress();
        MultipartFile[] multipartFile = emailDTO.getMultipartFile();


        Email email = null;


        //메일 첨부파일 등 처리, 리팩토링 필요 & 일부분만 구현
        if (Objects.equals(multipartFile[0].getOriginalFilename(), "")) {
            email = EmailBuilder.startingBlank()
                    .from(fromName, fromAddress)
                    .to(toAddress, toAddress)
                    .withSubject(subject)
                    .withHTMLText((String) content)
                    .buildEmail();

        } else if (multipartFile.length == 1) {
            email = EmailBuilder.startingBlank()
                    .from(fromName, fromAddress)
                    .to(toAddress, toAddress)
                    .withSubject(subject)
                    .withHTMLText((String) content)
                    .withAttachment(multipartFile[0].getOriginalFilename(), multipartFile[0].getBytes(), Objects.requireNonNull(multipartFile[0].getContentType()))
                    .buildEmail();
        } else if (multipartFile.length == 2) {
            email = EmailBuilder.startingBlank()
                    .from(fromName, fromAddress)
                    .to(toAddress, toAddress)
                    .withSubject(subject)
                    .withHTMLText((String) content)
                    .withAttachment(multipartFile[0].getOriginalFilename(), multipartFile[0].getBytes(), Objects.requireNonNull(multipartFile[0].getContentType()))
                    .withAttachment(multipartFile[1].getOriginalFilename(), multipartFile[1].getBytes(), Objects.requireNonNull(multipartFile[1].getContentType()))
                    .buildEmail();
        }

        Mailer inhouseMailer = MailerBuilder
                .withSMTPServer("mail.ddit.site", 25, fromAddress, "java")
                .buildMailer();

        inhouseMailer.sendMail(email);


    }

    @Override
    public EmailDTO replyMailRead(int id) {
        List<EmailDTO> inboxes = receiveEmailList("INBOX");
        EmailDTO dto = viewMail((id - 1), inboxes);
        dto.setMessageNumber(id);
        Address fromAddress1 = dto.getFromAddress();
        String s = fromAddress1.toString();
        String fromAddress = "from :";
        if (dto.getFromAddress().toString() == null || dto.getFromAddress().toString().equals("")) {
            fromAddress += " ";
        } else {
            fromAddress += dto.getFromAddress();
        }
        String toAddress = "to :";
        if (dto.getToList()[0].toString() == null || dto.getToList()[0].toString().equals("")) {
            toAddress += " ";
        } else {
            toAddress += dto.getToList()[0].toString();
        }

        dto.setContent("</br></br></br></br></br></br></br>"
                + "-----Original Message-----" + "</br>"
                + fromAddress + "<br>"
                + toAddress + "<br>"
                + "sent : " + dto.getSentDate() + "<br>"
                + "sbject : " + dto.getSubject() + "<br>"
                + dto.getContent());
        dto.setSubject("RE : " + dto.getSubject());
        dto.setToAddress(dto.getFromAddress().toString().split(" ")[1].replace("<", "").replace(">", ""));
        log.info("메일 읽었을 때 DTO 값" + String.valueOf(dto));
        return dto;
    }

    // 이메일 답장하기
    @Override
    public void replyMail(EmailDTO emailDTO) throws IOException {

        log.info("받아온 DTO 값" + String.valueOf(emailDTO));

        //현재 로그인한 사용자 정보(userNumber)를 가져옴
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Member member = null;
        try {
            member = ((AccountContext) authentication.getPrincipal()).getMember();
        } catch (ClassCastException e) {
        }
        Long userNumber = member.getUserNumber();
        String domain = "@ddit.site";

        String pop3Host = "mail.ddit.site";
        String storeType = "pop3";
        String user = (member.getMemberId() + domain);
        String password = "java";


        //1) 세션값 받아옴, 메일 수신은 pop3프로토콜로만 받기로 함.
        Properties properties = new Properties();
        properties.put("mail.pop3.host", pop3Host);
        Session emailSession = Session.getDefaultInstance(properties);

        //2) pop3서버로 연결 함.
        POP3Store emailStore = null;
        try {
            emailStore = (POP3Store) emailSession.getStore(storeType);
            emailStore.connect(user, password);
            //3) 받은 메일함에 폴더에 접근 한다.
            Folder[] folders = emailStore.getDefaultFolder().list("*");
            Folder emailFolder = emailStore.getFolder("INBOX");
            emailFolder.open(Folder.READ_ONLY);
            MimeMessage mimeMessage = (MimeMessage) emailFolder.getMessage(emailDTO.getMessageNumber());
            Address[] replyTo = mimeMessage.getReplyTo();
            log.info("----------------" + String.valueOf(replyTo));
            String s = replyTo[0].toString();
            log.info("----------------" + String.valueOf(s));

            Email email = EmailBuilder.replyingTo(mimeMessage)
                    .from(user + domain)
                    .prependText("")
                    .buildEmail();
            log.info("----------------" + email);

            Mailer inhouseMailer = MailerBuilder
                    .withSMTPServer("mail.ddit.site", 25, user + domain, "java")
                    .buildMailer();

            inhouseMailer.sendMail(email);

        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        }


    }


    @Override
    public void sentMailCopy(EmailDTO emailDTO) {

        //현재 로그인한 사용자 정보(userNumber)를 가져옴
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Member member = null;
        try {
            member = ((AccountContext) authentication.getPrincipal()).getMember();
        } catch (ClassCastException e) {
        }
        Long userNumber = member.getUserNumber();
        String domain = "@ddit.site";

        String imapHost = "mail.ddit.site";
        String storeType = "imap";
        String user = (member.getMemberId() + domain);
        String password = "java";


        try {
            //1) 세션값 받아옴
            Properties properties = new Properties();
            properties.put("mail.host", imapHost);
            Session emailSession = Session.getDefaultInstance(properties);


            Message message = new MimeMessage(emailSession);


            message.addRecipient(
                    Message.RecipientType.TO,
                    new InternetAddress(emailDTO.getToAddress()));

            InternetAddress internetAddress = new InternetAddress();
            internetAddress.setAddress(user);
            Address address;
            address = (Address) internetAddress;

            message.setFrom(address);

            message.setSubject(emailDTO.getSubject());

            BodyPart messageBodyPart = new MimeBodyPart();

            messageBodyPart.setText(emailDTO.getContent().toString());

            // Create a multipar message
            Multipart multipart = new MimeMultipart();

            // Set text message part
            multipart.addBodyPart(messageBodyPart);

//            // Part two is attachment
//            messageBodyPart = new MimeBodyPart();
//
//            //Add the attachment file path
//            String filename = "C:/temp/bb.log";
//            FileDataSource source = new FileDataSource(filename);
//            messageBodyPart.setDataHandler(new DataHandler(source));
//            messageBodyPart.setFileName(filename);
//            multipart.addBodyPart(messageBodyPart);

            // Send the complete message parts
            message.setContent(multipart);


            //2) imap서버로 연결 함.
            IMAPStore emailStore = (IMAPStore) emailSession.getStore(storeType);
            emailStore.connect(user, password);

            emailStore.getFolder("Sent");

            Folder draftsFolder = emailStore.getFolder("Sent");
            if (!draftsFolder.exists()) {
                draftsFolder.create(1);
                draftsFolder.renameTo(draftsFolder);
                draftsFolder.close();
            }
            Folder drafts = emailStore.getFolder("Sent");
            drafts.open(Folder.READ_WRITE);

            drafts.appendMessages(new Message[]{message});

        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void tempMail(EmailDTO emailDTO) throws IOException {

        //현재 로그인한 사용자 정보(userNumber)를 가져옴
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Member member = null;
        try {
            member = ((AccountContext) authentication.getPrincipal()).getMember();
        } catch (ClassCastException e) {
        }
        Long userNumber = member.getUserNumber();
        String domain = "@ddit.site";

        String imapHost = "mail.ddit.site";
        String storeType = "imap";
        String user = (member.getMemberId() + domain);
        String password = "java";


        try {
            //1) 세션값 받아옴
            Properties properties = new Properties();
            properties.put("mail.host", imapHost);
            Session emailSession = Session.getDefaultInstance(properties);


            Message message = new MimeMessage(emailSession);


            message.addRecipient(
                    Message.RecipientType.TO,
                    new InternetAddress(emailDTO.getToAddress()));

            InternetAddress internetAddress = new InternetAddress();
            internetAddress.setAddress(user);
            Address address;
            address = (Address) internetAddress;

            message.setFrom(address);

            message.setSubject(emailDTO.getSubject());

            BodyPart messageBodyPart = new MimeBodyPart();

            messageBodyPart.setText(emailDTO.getContent().toString());

            // Create a multipar message
            Multipart multipart = new MimeMultipart();

            // Set text message part
            multipart.addBodyPart(messageBodyPart);

//            // Part two is attachment
//            messageBodyPart = new MimeBodyPart();
//
//            //Add the attachment file path
//            String filename = "C:/temp/bb.log";
//            FileDataSource source = new FileDataSource(filename);
//            messageBodyPart.setDataHandler(new DataHandler(source));
//            messageBodyPart.setFileName(filename);
//            multipart.addBodyPart(messageBodyPart);

            // Send the complete message parts
            message.setContent(multipart);


            //2) imap서버로 연결 함.
            IMAPStore emailStore = (IMAPStore) emailSession.getStore(storeType);
            emailStore.connect(user, password);

            emailStore.getFolder("Drafts");

            Folder draftsFolder = emailStore.getFolder("Drafts");
            if (!draftsFolder.exists()) {
                draftsFolder.create(1);
                draftsFolder.renameTo(draftsFolder);
                draftsFolder.close();
            }
            Folder drafts = emailStore.getFolder("Drafts");
            drafts.open(Folder.READ_WRITE);

            drafts.appendMessages(new Message[]{message});

        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void moveMail(String folderName, Long mailUID, String TargetFolderName) {

        //현재 로그인한 사용자 정보(userNumber)를 가져옴
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Member member = null;
        try {
            member = ((AccountContext) authentication.getPrincipal()).getMember();
        } catch (ClassCastException ignored) {
        }
        Long userNumber = member.getUserNumber();
        String domain = "@ddit.site";

        String imapHost = "mail.ddit.site";
        String storeType = "imap";
        String user = (member.getMemberId() + domain);
        String password = "java";


        try {
            //1) 세션값 받아옴
            Properties properties = new Properties();
            properties.put("mail.host", imapHost);
            Session emailSession = Session.getDefaultInstance(properties);

            Message message = new MimeMessage(emailSession);

            //2) imap서버로 연결 함.
            IMAPStore emailStore = (IMAPStore) emailSession.getStore(storeType);
            emailStore.connect(user, password);

            emailStore.getFolder(folderName);

            IMAPFolder folder = (IMAPFolder) emailStore.getFolder(folderName);
            folder.open(Folder.READ_WRITE);
            Message messageByUID = folder.getMessageByUID(mailUID);

            Folder toFolder = emailStore.getFolder(TargetFolderName);
            if (!toFolder.exists()) {
                toFolder.create(1);
                toFolder.renameTo(toFolder);
                toFolder.close();
            }
            Folder newToFolder = emailStore.getFolder(TargetFolderName);

            folder.moveUIDMessages(new Message[]{messageByUID}, newToFolder);


        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
