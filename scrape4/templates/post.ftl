<#setting number_format="computer">
<!DOCTYPE html>
<html lang="en">
   <head>
      <meta charset="utf-8">
      <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
      <link rel="stylesheet" href="../styles/bootstrap.min.css">
      <link rel="stylesheet" href="../styles/grid.css">
      <link rel="stylesheet" href="../styles/vert.css">
      <script src="../scripts/script.js"></script>
      <title>${title}</title>
   </head>
   <body class="bg-dark" onload="getbacklinks()">
   <form method="POST" target="_blank" action="../deletefiles.php">
      <div class="card border-white mb-4 mt-2 ml-2 mr-2">
         <h6 class="card-header bg-secondary text-center">
            <#if firstpost.isClosed()>
            <a href="${livethreadurl}" class="h3 text-success" target="_blank">${header}</a>
            <#else>
            <a href="${livethreadurl}" class="h3 text-white" target="_blank">${header}</a>
            </#if>
         </h6>
         <a id="p${firstpost.no}"></a>
         <div class="card-body">
            <ul class="list-group list-group-flush">
               <#if firstpost.md5??>
               <li class="list-group-item text-center">
                  <figure class="figure shadow p-3 mb-5 bg-white rounded">
                     <a href="../images/${firstpost.sanitizedMD5}${firstpost.ext}" target="_blank">
                        <#if firstpost.ext == ".webm" || firstpost.ext == ".gif">
                        <img src="../images/${firstpost.sanitizedMD5}s.jpg" width="${firstpost.tn_w}px" height="${firstpost.tn_h}px" alt="${firstpost.md5}">
                        <#else>
                        <img src="../images/${firstpost.sanitizedMD5}${firstpost.ext}" width="${firstpost.tn_w}px" height="${firstpost.tn_h}px" alt="${firstpost.md5}">
                        </#if>
                     </a>
                     <figcaption class="figure-caption">
                        <span title="${firstpost.filename}${firstpost.ext}">${firstpost.shortfilename}${firstpost.ext}</span>
                     </figcaption>
                     <input type="checkbox" id="imgchk" name="images[]" value="images/${firstpost.sanitizedMD5}${firstpost.ext}">
                  </figure>
               </li>
               </#if>
               <#if firstpost.com??>
               <li class="list-group-item text-center">
                  <figcaption class="figure-caption">
                     <#if firstpost.name??>${firstpost.name}</#if>
                  </figcaption>
                  <figure class="figure text-left">
                     <div class="card example-1 pl-1 pr-1 square scrollbar-cyan bordered-cyan">
                        <p class="card-text small">
                           ${firstpost.com}
                        </p>
                     </div>
                  </figure>
               </li>
               </#if>
               <#list urllist>
               <li class="list-group-item text-center">
                  <figcaption class="figure-caption">
                     Youtube links
                  </figcaption>
                  <figure class="figure text-left">
                     <div class="card example-2 pl-1 pr-1 square scrollbar-cyan bordered-cyan">
                        <p class="card-text small">
                           <#items as url>
                           ${url?counter}. <a href="https://${url}" target="_blank">${url}</a><br>
                           </#items>
                        </p>
                     </div>
                  </figure>
               </li>
               </#list>
            </ul>
         </div>
         <#list posts>
         <div class="card-body ml-5 mr-5 pl-5 pr-5">
            <button type="button" onclick="selectall()" class="btn ml-5 mr-1 mb-1 border btn-outline-light btn-sm"><span class="badge badge-info">&forall;</span></button>
            <button type="button" onclick="deselectall()" class="btn mr-1 mb-1 border btn-outline-light btn-sm"><span class="badge badge-primary">&empty;</span></button>
            <button type="submit" class="btn mb-1 border btn-outline-light btn-sm"><span class="badge badge-danger">&times;</span></button>
            <div class="row ml-5 mr-5 pl-5 pr-5" style="background-color:rgba(158, 0, 255, 0.12)">
               <#items as post>
               <#if post.md5??>
               <#if post?index gt 0>
                   <!-- ${post.md5} -->
	               <#if post.ext == ".webm">
	               <div class="media text-center shadow ml-3 mb-5 mt-5 bg-white border border-primary rounded">
	                  <div class="verticaltext_webm">
	                     <a href="#p${post.no}">
	                     <div class="verticaltext_content_webm">
	                        ${post.ext}
	                     </div>
	                     </a>
	                  </div>
	                  <span title="${post.filename}${post.ext}">
	                     <a href="../images/${post.sanitizedMD5}${post.ext}" target="_blank">
	                        <img class="ml-3 m-1 border border-secondary small" src="../images/${post.sanitizedMD5}s.jpg" width="${post.tn_w}px" height="${post.tn_h}px" alt="&ulcorn;&drcorn;">
	                     </a>
	                  </span>
	                  <input type="checkbox" class="mr-1 mt-1" id="imgchk" name="images[]" value="images/${post.sanitizedMD5}${post.ext}">
	               </div>
	               <#elseif post.ext == ".gif">
	               <div class="media text-center shadow ml-3 mb-5 mt-5 bg-light border border-success rounded">
	                  <div class="verticaltext_gif">
	                     <a href="#p${post.no}">
	                     <div class="verticaltext_content_gif">
	                        ${post.ext}
	                     </div>
	                     </a>
	                  </div>
	                  <span title="${post.filename}${post.ext}">
	                     <a href="../images/${post.sanitizedMD5}${post.ext}" target="_blank">
	                        <img class="ml-3 m-1 border border-secondary small" src="../images/${post.sanitizedMD5}s.jpg" width="${post.tn_w}px" height="${post.tn_h}px" alt="&ulcorn;&drcorn;">
	                     </a>
	                  </span>
	                  <input type="checkbox" class="mr-1 mt-1" id="imgchk" name="images[]" value="images/${post.sanitizedMD5}${post.ext}">
	               </div>
	               <#elseif post.ext == ".png">
	               <div class="media text-center shadow-sm ml-3 mb-5 mt-5 bg-white rounded">
	                  <div class="verticaltext_3">
	                     <a href="#p${post.no}">
	                     <div class="verticaltext_content_png">
	                        ${post.ext}
	                     </div>
	                     </a>
	                  </div>
	                  <span title="${post.filename}${post.ext}">
	                     <a href="../images/${post.sanitizedMD5}${post.ext}" target="_blank">
	                        <img class="ml-3 m-1 border border-white small" src="../images/${post.sanitizedMD5}${post.ext}" width="${post.tn_w}px" height="${post.tn_h}px" alt="&ulcorn;&drcorn;">
	                     </a>
	                  </span>
	                  <input type="checkbox" class="mr-1 mt-1" id="imgchk" name="images[]" value="images/${post.sanitizedMD5}${post.ext}">
	               </div>
	               <#elseif post.ext == ".jpg">
	               <div class="media text-center shadow-sm ml-3 mb-5 mt-5 bg-white rounded">
	                  <div class="verticaltext_3">
	                     <a href="#p${post.no}">
	                     <div class="verticaltext_content_jpg">
	                        ${post.ext}
	                     </div>
	                     </a>
	                  </div>
	                  <span title="${post.filename}${post.ext}">
	                     <a href="../images/${post.sanitizedMD5}${post.ext}" target="_blank">
	                        <img class="ml-3 m-1 border border-white small" src="../images/${post.sanitizedMD5}${post.ext}" width="${post.tn_w}px" height="${post.tn_h}px" alt="&ulcorn;&drcorn;">
	                     </a>
	                  </span>
	                  <input type="checkbox" class="mr-1 mt-1" id="imgchk" name="images[]" value="images/${post.sanitizedMD5}${post.ext}">
	               </div>
	               <#else>
	               <div class="media text-center shadow-sm ml-3 mb-5 mt-5 bg-white rounded">
	                  <div class="verticaltext_3">
	                     <a href="#p${post.no}">
	                     <div class="verticaltext_content_3">
	                        ${post.ext}
	                     </div>
	                     </a>
	                  </div>
	                  <span title="${post.filename}${post.ext}">
	                     <a href="../images/${post.sanitizedMD5}${post.ext}" target="_blank">
	                        <img class="ml-3 m-1 border border-white small" src="../images/${post.sanitizedMD5}${post.ext}" width="${post.tn_w}px" height="${post.tn_h}px" alt="&ulcorn;&drcorn;">
	                     </a>
	                  </span>
	                  <input type="checkbox" class="mr-1 mt-1" id="imgchk" name="images[]" value="images/${post.sanitizedMD5}${post.ext}">
	               </div>
	               </#if>
               </#if>
               </#if>
               </#items>
            </div>
         </div>
         </#list>
         <div class="card-body">
            <#list posts as post>
            <#if post?index gt 0>
            <a id="p${post.no}"></a>
            <div class="container mt-2">
               <div class="row">
					<div class="d-flex flex-column bg-light pr-3 pl-3 border border-secondary text-center">
						<div class="text-muted mb-2">
							>><a href="https://${domain}/${board}/thread/${post.resto}#p${post.no}" class="postid text-muted" target="_blank">${post.no}</a>
							<strong class="d-inline-block text-success"><#if post.name??>${post.name}</#if></strong>
						</div>
						<div id="bl${post.no}" class="small text-left border-bottom"></div>
						<div class="d-flex flex-row">
							<#if post.md5??>
							<div class="col-md-auto">
								<figure class="figure text-center shadow-sm p-3 bg-white rounded">
									<figcaption class="figure-caption">
										<span title="${post.filename}${post.ext}">${post.shortfilename}${post.ext}</span>
									</figcaption>
									<a href="../images/${post.sanitizedMD5}${post.ext}" target="_blank">
										<#if post.ext == ".webm" || post.ext == ".gif">
										<img src="../images/${post.sanitizedMD5}s.jpg" width="${post.tn_w}px" height="${post.tn_h}px" alt="${post.md5}">
										<#else>
										<img src="../images/${post.sanitizedMD5}${post.ext}" width="${post.tn_w}px" height="${post.tn_h}px" alt="${post.md5}">
										</#if>
									</a>
									<figcaption class="figure-caption">
										${post.w}x${post.h}, ${post.filesize}
									</figcaption>
								</figure>
							</div>
							</#if>
							<#if post.com??>
							<div class="card example-1 pl-2 pr-2 mb-4 mt-4 square scrollbar-cyan bordered-cyan bg-light border-0">
								<p class="card-text mb-auto small text-left" id="${post.no}">
									${post.com}
								</p>
							</div>
							</#if>
						</div>
						<div class="text-muted">
							${post.now}
						</div>
					</div>
               </div>
            </div>
            </#if>
            </#list>
         </div>
         <div class="card-footer bg-secondary text-white text-center">${lastmodified}</div>
      </div>
   </form>
   </body>
</html>