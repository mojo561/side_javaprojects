function selectall()
{
        var boxes = document.getElementsByTagName("input");
        for(var i = 0; i < boxes.length; i++)
        {
                if(boxes[i].type === "checkbox")
                {
                        if(boxes[i].checked == false)
                        {
                                boxes[i].checked = true;
                        }
                }
        }
}

function deselectall()
{
        var boxes = document.getElementsByTagName("input");
        for(var i = 0; i < boxes.length; i++)
        {
                if(boxes[i].type === "checkbox")
                {
                        if(boxes[i].checked == true)
                        {
                                boxes[i].checked = false;
                        }
                }
        }
}

//https://stackoverflow.com/questions/1960473/get-all-unique-values-in-a-javascript-array-remove-duplicates/14438954#14438954
function onlyUnique(value, index, self)
{
        return self.indexOf(value) == index;
}

function getbacklinks()
{
  var quotes = document.getElementsByClassName("quotelink");
  var posts = document.getElementsByClassName("postid");
  
  for(var i = 0; i < posts.length; i++)
  {
      var postid = posts[i].innerHTML;
      var pidarray = [];
      for(var j = 0; j < quotes.length; j++)
      {
          var pid = quotes[j].innerHTML.replace(/[^0-9]/g, "");
          if(pid === postid)
          {
              pidarray.push(quotes[j].parentNode.id);
          }
      }
      var bltarget = document.getElementById("bl" + postid);
      if(bltarget != null)
      {
          pidarray = pidarray.filter(onlyUnique);
          for(var j = 0; j < pidarray.length; j++)
          {
              var node = document.createElement("A");
              node.href = "#p" + pidarray[j];
              node.innerHTML = ">>" + pidarray[j];
              bltarget.appendChild(node);
              if((j+1) % 15 == 0)
              {
                  bltarget.appendChild(document.createElement("BR"));
              }
          }
      }
  }
}