<!DOCTYPE html>
<html>
    <head>
      <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    </head>
    <body>
      <p>Hi,</p>
      <p>${initiatorEmail} sent you a sign request for the following document list :</p>
      <ul>
        <#list documentList as document>
          <li>${document.originalFileName}</li>
        </#list>
      </ul>
      <p>You can sign this document(s) by following this link <a href="${taskUrl}">${taskUrl}</a></p>
    </body>
</html>
