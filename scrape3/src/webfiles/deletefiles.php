<!DOCTYPE html>
<html>
<body>
<?php
if(!empty(_POST))
{
   if(isset($_POST["images"]))
   {
      foreach($_POST["images"] as $img)
      {
         echo "<dl>\n";
         echo "<dt>Attempting to unlink $img</dt>\n";
         if(unlink($img))
         {
            echo "<dd>Ok</dd>";
         }
         else
         {
            echo "<dd><b>Couldn't delete the file</b></dd>";
         }
         if(strpos($img, ".gif") !== false)
         {
            $thumb = str_replace(".gif", "s.jpg", $img);
            if(unlink($thumb))
            {
               echo "<dd>Deleted thumbnail too</dd>";
            }
            else
            {
               echo "<dd><b>Couldn't delete the file</b></dd>";
            }
         }
         else if(strpos($img, ".webm") !== false)
         {
            $thumb = str_replace(".webm", "s.jpg", $img);
            if(unlink($thumb))
            {
               echo "<dd>Deleted thumbnail too</dd>";
            }
            else
            {
              echo "<dd><b>Couldn't delete the file</b></dd>";
            }
         }
         echo "</dl>\n";
      }
   }
}
?>
</body>
</html>