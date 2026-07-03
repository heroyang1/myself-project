package com.backstage.common.utils.email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 九转苍翎
 * Date: 2026/3/30
 * Time: 15:44
 */
@Component
public class EmailUtil {
    // 邮件主题
    public final String subject = "open source helper";
    // 发件人
    public final String from = "18482663265@163.com";
    // 收件人
    public final String[] to = new String[]{"3210728077@qq.com","3891715998@qq.com"};
    @Autowired
    private JavaMailSender javaMailSender;
    @Autowired
    private SpringTemplateEngine springTemplateEngine;

    /**
     * 发送邮件
     * @param username 用户名
     * @param email 用户邮箱
     * @return 唯一标识
     */
    public String sendEmailGetUniqueId(String username, String email) throws MessagingException {
        return sendEmailGetUniqueId(username, email, null, null);
    }

    public String sendEmailGetUniqueId(String username, String email, String redeemCode, String productName) throws MessagingException {
        checkEmail(email);
        // 准备模板数据
        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("message", "欢迎使用我们的服务！");
        context.setVariable("subtitle", redeemCode == null ? "感谢您注册开源助手 · 开启开源之旅" : "请完成注册，并在登录后手动核销您购买的资源");
        String uniqueId = UUID.randomUUID() + "-" + System.currentTimeMillis();
        context.setVariable("uniqueId", uniqueId);
        context.setVariable("redeemCode", redeemCode);
        context.setVariable("productName", productName);
        sendUniqueIdTemplate(subject, email, context);
        return uniqueId;
    }

    public void sendAdminInviteEmail(String email, String roleName, String inviteLink, long expireDays, String expireTime, Long initialPoints) throws MessagingException {
        checkEmail(email);
        Context context = new Context();
        context.setVariable("username", "受邀用户");
        context.setVariable("subtitle", "您被邀请成为平台「" + roleName + "」");
        context.setVariable("inviteLink", inviteLink);
        context.setVariable("roleName", roleName);
        context.setVariable("expireDays", expireDays);
        context.setVariable("expireTime", expireTime);
        context.setVariable("initialPoints", initialPoints);
        sendUniqueIdTemplate("您被邀请成为平台" + roleName, email, context);
    }

    public void sendAdminInviteRegisterSuccessEmail(String username, String email, String uniqueId, String roleName) throws MessagingException {
        checkEmail(email);
        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("subtitle", "您已成功注册为平台「" + roleName + "」");
        context.setVariable("uniqueId", uniqueId);
        context.setVariable("roleName", roleName);
        context.setVariable("adminInviteSuccess", true);
        sendUniqueIdTemplate("注册成功通知", email, context);
    }

    private void sendUniqueIdTemplate(String mailSubject, String email, Context context) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);
        String content = springTemplateEngine.process("GetUniqueId", context);
        mimeMessageHelper.setSubject(mailSubject);
        mimeMessageHelper.setText(content, true);
        mimeMessageHelper.setTo(email);
        mimeMessageHelper.setFrom(from);
        ClassPathResource imageResource = new ClassPathResource("static/open-source-helper.jpg");
        mimeMessageHelper.addInline("logoImage", imageResource);
        javaMailSender.send(mimeMessage);
    }
    /**
     * 发送普通邮件
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    public void sendEmailCommon(String subject, String content) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);
        // 准备模板数据
        Context context = new Context();
        context.setVariable("subject", subject);
        context.setVariable("content", content);
        context.setVariable("sendTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        // 通过模板引擎生成HTML内容
        String html = springTemplateEngine.process("Common", context);
        // 设置邮件内容
        mimeMessageHelper.setSubject(subject);
        mimeMessageHelper.setText(html, true);
        mimeMessageHelper.setTo(to);
        mimeMessageHelper.setFrom(from);
        //
        ClassPathResource imageResource = new ClassPathResource("static/open-source-helper.jpg");
        mimeMessageHelper.addInline("logoImage", imageResource);
        javaMailSender.send(mimeMessage);
    }
    /**
     * 发送新网站提交通知邮件
     * @param websiteName 网站名称
     * @param websiteUrl 网站链接
     * @param websiteDescription 网站描述
     * @param submitter 提交人
     * @param submitTime 提交时间
     */
    @Async
    public void sendNewWebsiteSubmitEmail(
            Long websiteId,
            String websiteName,
            String websiteUrl,
            String websiteDescription,
            String submitter,
            String submitTime
    ) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);

        // 准备模板数据
        Context context = new Context();
        context.setVariable("websiteId", websiteId);
        context.setVariable("websiteName", websiteName);
        context.setVariable("websiteUrl", websiteUrl);
        context.setVariable("websiteDescription", websiteDescription);
        context.setVariable("submitter", submitter);
        context.setVariable("submitTime", submitTime);
        // 审核链接
        context.setVariable("auditUrl",  "http://localhost:8080/api/website/audit/detail/" + websiteId);

        // 通过模板引擎生成HTML内容
        String content = springTemplateEngine.process("SubmitWebsites", context);

        // 设置邮件内容
        mimeMessageHelper.setSubject("【新网站提交】" + websiteName);
        mimeMessageHelper.setText(content, true);
        mimeMessageHelper.setTo(to);  // 发送给所有管理员
        mimeMessageHelper.setFrom(from);

        //
        ClassPathResource imageResource = new ClassPathResource("static/open-source-helper.jpg");
        mimeMessageHelper.addInline("logoImage", imageResource);

        // 发送邮件
        javaMailSender.send(mimeMessage);
    }

    public void checkEmail(String email) {
        String regex = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
        if (!email.matches(regex)) {
            throw new RuntimeException("邮箱格式错误");
        }
    }
}
