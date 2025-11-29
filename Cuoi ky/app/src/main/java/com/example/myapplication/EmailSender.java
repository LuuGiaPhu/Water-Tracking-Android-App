package com.example.myapplication;

        import java.util.Properties;
        import javax.mail.*;
        import javax.mail.internet.InternetAddress;
        import javax.mail.internet.MimeMessage;

        public class EmailSender {

            private static final String SENDER_EMAIL = "hiroyamasaki0939@gmail.com";
            private static final String SENDER_PASSWORD = "chps gdzm xcyc iprl";
            private static final String SMTP_HOST = "smtp.gmail.com";
            private static final String SMTP_PORT = "587";

            public void sendEmail(String to, String subject, String bodyText) throws MessagingException {
                Properties props = new Properties();
                props.put("mail.smtp.host", SMTP_HOST);
                props.put("mail.smtp.port", SMTP_PORT);
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.auth", "true");

                Session session = Session.getInstance(props, new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SENDER_EMAIL));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
                message.setSubject(subject);
                message.setText(bodyText);

                Thread thread = new Thread(() -> {
                    try {
                        Transport.send(message);
                    } catch (MessagingException e) {
                        e.printStackTrace();
                    }
                });
                thread.start();
            }
        }