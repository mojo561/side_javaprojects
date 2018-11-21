<!DOCTYPE html>
<html>
<head>
<title>Delete A File</title>
</head>
<body>
<dl>
<?php
if(!empty(_POST))
{
   if(isset($_POST["filesrc"]))
   {
      echo "<dt>Attempting to delete " . $_POST["filesrc"] . "</dt>\n";
      if(unlink($_POST["filesrc"]))
      {
         echo "<dd>Ok</dd>";
      }
      else
      {
         echo "<dd><b>Couldn't delete the file...</b></dd>";
      }
   }
   if(isset($_POST["filethumb"]))
   {
      echo "<dt>Attempting to delete " . $_POST["filethumb"] . "</dt>\n";
      if(unlink($_POST["filethumb"]))
      {
         echo "<dd>Ok</dd>";
      }
      else
      {
         echo "<dd><b>Couldn't delete the file...</b></dd>";
      }
   }
}
?>
</dl>
</body>
</html>