<!DOCTYPE html>
<html>
    <head>
      <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    </head>
    <body>
      <p>Hi,</p>
      <p>The document(s) you digitally signed have been downloaded by ${task.initiator.email}</p>
      <p>Document list :</p>
      <ul>
        <#list task.documentList as document>
          <li>${document.originalFileName}</li>
        </#list>
      </ul>
    </body>
</html>
