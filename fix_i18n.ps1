# Fix 1: Add user.* i18n keys
$i18nFile = 'C:\Users\Admin1\IdeaProjects\osh-backend\backstage-admin\src\main\resources\i18n\messages.properties'
$content = [System.IO.File]::ReadAllText($i18nFile, [System.Text.Encoding]::UTF8)
$additions = "`n# user.* aliases (SysLoginService references these keys)`nuser.login.success=`u767B\u5F55\u6210\u529F`nuser.jcaptcha.error=`u9A8C\u8BC1\u7801\u9519\u8BEF`nuser.jcaptcha.expire=`u9A8C\u8BC1\u7801\u5DF2\u5931\u6548`nuser.password.not.match=`u7528\u6237\u4E0D\u5B58\u5728/`u5BC6\u7801\u9519\u8BEF`nuser.forceout=`u7BA1\u7406\u5458\u5F3A\u5236\u9000\u51FA`uFF0C\u8BF7\u91CD\u65B0\u767B\u5F55`n"
if ($content -notmatch 'user\.login\.success') {
    $content = $content.TrimEnd() + "`n" + $additions
    [System.IO.File]::WriteAllText($i18nFile, $content, [System.Text.UTF8Encoding]::new($false))
    Write-Host '[OK] Added user.* i18n keys'
} else {
    Write-Host '[SKIP] user.login.success already exists'
}
