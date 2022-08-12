<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>页面数据更新</title>
    <%@include file="../../commons/header.jsp" %>
    <%@include file="../../commons/allScript.jsp" %>
    <script type="text/javascript">
      $(function () {
        var validate = $("#updateForm").validate({
          submitHandler: function (form) {
            $.post("${pageContext.request.contextPath}/commons/webpage/updateWebpageInfo", $("#updateForm").serialize(), function (json) {
              if (json.success) {
                showModal("保存成功", json.result, function () {
                  $('#confirmModal').modal('hide');
                }, function () {
                  $('#confirmModal').modal('hide');
                });
              } else {
                showModal("保存失败", json.errorMsg, function () {
                  $('#confirmModal').modal('hide');
                }, function () {
                  $('#confirmModal').modal('hide');
                });
              }
            });
          },
          rules: {
            id: {
              required: false
            },
            title: {
              required: false
            },
            contentCleaned: {
              required: false
            },
            url: {
              required: true
            },
            domain: {
              required: false
            },
            domainName: {
              required: false
            },
            keywords: {
              required: false
            },
            level: {
              required: false
            }
          },
          highlight: function (element) {
            $(element).closest('.form-group').addClass('has-error');
          },
          success: function (label) {
            label.closest('.form-group').removeClass('has-error');
            label.remove();
          },
          errorPlacement: function (error, element) {
            element.parent('div').append(error);
          }
        });
      });
    </script>
</head>
<body>
<%@include file="../../commons/head.jsp" %>
<div class="container">
    <form id="updateForm">
        <div class="form-group">
            <label for="id">ID，新增时请勿填写，将自动生成</label>
            <input type="text" class="form-control" id="id" name="id" placeholder="ID"
                   value="${webpage.id}">
        </div>
        <div class="form-group">
            <label for="title">标题</label>
            <input type="text" class="form-control" id="title" name="title" placeholder="标题"
                   value="${webpage.title}">
        </div>
        <div class="form-group">
            <label for="contentCleaned">内容</label>
            <textarea class="form-control" id="contentCleaned" name="contentCleaned" rows="15">
                ${webpage.contentCleaned}</textarea>
        </div>
        <div class="form-group">
            <label for="url">url链接</label>
            <input type="text" class="form-control" id="url" name="url" placeholder="url链接"
                   value="${webpage.url}">
        </div>
        <div class="form-group">
            <label for="domain">域名</label>
            <input type="text" class="form-control" id="domain" name="domain" placeholder="域名"
                   value="${webpage.domain}">
        </div>
        <div class="form-group">
            <label for="domainName">域名中文名(如果已有域名信息，可以不填写，通过域名直接生成)</label>
            <input type="text" class="form-control" id="domainName" name="domainName" placeholder="域名中文名"
                   value="${webpage.domainName}">
        </div>
        <div class="form-group">
            <label for="keywords">关键词(使用空格分隔)</label>
            <input type="text" class="form-control" id="keywords" name="keywords" placeholder="关键词">
        </div>
        <div class="form-group">
            <label for="level">级别(普通级别为 0，内部文件为 10)</label>
            <input type="number" class="form-control" id="level" name="level" placeholder="级别"
                   value="${webpage.level}">
        </div>
        <div class="form-group">
            <label for="publishTime">发布日期(不填写则为现在，使用yyyy-MM-dd格式)</label>
            <input type="text" class="form-control" id="publishTime" name="publishTime" placeholder="发布时间"
            value="<fmt:formatDate value="${webpage.publishTime}" pattern="yyyyMMdd"/>">
        </div>
        <button type="submit" class="btn btn-danger">提交</button>
    </form>
</div>
</body>
<script>
  var keywordList = [];
  <c:forEach items="${webpage.keywords}" var="keyword">
  keywordList.push('${keyword}');
  </c:forEach>
  $('#keywords').val(keywordList.join(" "));
</script>
</html>