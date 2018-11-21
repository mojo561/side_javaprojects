<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
<link rel="stylesheet" href="styles/bootstrap.min.css">
<link rel="stylesheet" href="styles/grid.css">
<!-- <link rel="stylesheet" href="styles/vert.css"> -->
<title>Scrape3 Index</title>
</head>
<body>
<h1 class="display-1 text-center">Scrape 3</h1>
<?php
$dir = ".";
if (is_dir($dir))
{
   if ($dh = opendir($dir))
   {
      while (($file = readdir($dh)) !== false)
      {
         if(is_dir($file) && $file != "." && $file != ".." && $file != "images" && $file != "styles" && $file != "scripts")
         {
            $dirlist = array_diff( scandir($file) , array('..','.') );
            if(count($dirlist) > 0)
            {
               echo "<div class=\"d-flex justify-content-center mt-3 mb-5\">";
               echo "<div>";
               echo "<table class=\"table-bordered table-sm table-striped\">";
               echo "<thead><tr><th colspan=3 class=\"text-center bg-dark text-light text-uppercase\"><h4>&raquo; $file</h4></th></tr></thead>";
               echo "<tbody class=\"small\">";
               foreach($dirlist as $f)
               {
                  echo "<tr>";
                  echo "<td class=\"bg-danger\">";
                  echo "<form action=deleteafile.php method=POST target=_blank>";
                  echo "<button type=submit class=\"close pb-1\"><span aria-hidden=\"true\">&times;</span></button>";
                  echo "<input type=hidden name=filesrc value=\"$file/$f\">";
                  echo "</form>";
                  echo "</td>";
                  echo "<td>" . date('m/d h:i A', filemtime("$file/$f")) . "</td>";
                  echo "<td><a href=\"$file/$f\" target=\"_blank\">$f</a></td>";
                  echo "</tr>";
               }
               echo "</tbody>";
               echo "</table>";
               echo "</div>";
               echo "</div>\n";
            }
         }
      }
   }
   closedir($dh);
}
?>
</body>
</html>