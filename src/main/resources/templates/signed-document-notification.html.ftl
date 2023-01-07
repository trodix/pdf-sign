<!DOCTYPE html>
<html>
    <head>
      <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    </head>
    <body>
      <p>Hi,</p>
      <p>Your document(s) have been digitally signed</p>
      <p>Document list :</p>
      <ul>
        <#list task.documentList as document>
          <li>${document.originalFileName}</li>
        </#list>
      </ul>
      <p>Signature history :</p>
      <ul>
        <#list task.signatureHistory as entry>
          <li>${entry.signedBy.email}</li>
          <li>${entry.signedAt}</li>
        </#list>
      </ul>
      <p>You can download your document(s) by following this link <a href="${downloadUrl}">${downloadUrl}</a></p>
    </body>
</html>
