/* Shared Functions */

/* password checks */

$(document).ready(function () {
  $('form').on('input','input[type=password]',function (e) {
    var pwds = $(this).closest("form").find("input[type=password]");
    pwds[0].setCustomValidity('');
    pwds[1].setCustomValidity('');
    if ($(pwds[0]).val() != $(pwds[1]).val()) {
      this.setCustomValidity("Passwords must match");
    }
  });
});

/* image load */

const aoscpath = "https://assets.warhammerchampions.com/card-database/cards/";
const localpath = "/img/aosc/cards/";
var _imgpath = localpath;

_collection = $('#collection').val();
_collection = (_collection != "" ? JSON.parse(_collection) : {});

//function setPath (uri, callback) {
//  var $img = $('<img></img>');
//  $img.on('load',function() {
//    _imgpath = localpath;
//    callback;
//  });
//  $img.on('error',function() {
//    _imgpath = aoscpath;
//    callback;
//  });
//  
//  $img.attr('src',uri);
//}

/* Parse Filter */
// name

//a: alliance
//c: class
//w: category
//t tags
//r rarity

//o: cost
//h: health
//u: true/false (unique)

//x: Text (one word)
//NOT IMPLEMENTED: x: "Some Text" (quotes)
function parsefilter(f)  {
  var res;
  var outp = {};
  
  res = /a:(.+?\s|.+)/.exec(f);
  if (res != null) { 
    outp.alliance = {"likenocase":res[1].trim().split("|")}; 
  }
  res = /c:(\w+)/.exec(f);
  if (res != null) {
    outp.category = {"en":[res[1]]};
  }
  
  res = /w:(\w+)/.exec(f);
  if (res != null) {
    outp.class={"en":[res[1].trim()]};
  }
  res = /w:\"(.+)\"/.exec(f);
  if (res != null) {
    outp.class={"en":[res[1].trim()]};
  }
  
  res = /t:(.+?\s|.+)/.exec(f);
  if (res != null) {
    outp["tags"] = {"has":res[1].trim().split('|')};
  }
  
  res = /r:(\w+)/.exec(f);
  if (res != null) {
    outp.rarity = {"isnocase":res[1]};
  }
  
  var cost = /o(\S)([0-9]+)/;
  res = RegExp(cost).exec(f);
  if (res != null)  {
    outp["cost"] = getNumberFilter(res[1],res[2]);
  }
  var health = /h(\S)(-?[0-9])/;
  res = RegExp(health).exec(f);
  if (res != null)  {
    outp["healthMod"] = getNumberFilter(res[1],res[2]);
  }
  
  res = /x:(.+)/.exec(f);
  if (res != null) {
    outp["text"] = {likenocase:res[1]};
  }
  res = /x:\"(.+)\"/.exec(f);
  if (res != null) {
    outp["text"] = {likenocase:res[1]};
  }
  
  
  var unique = /u:(true|false)/;
  res = RegExp(unique).exec(f);
  if (res != null) {
    outp["tags"] = (res[1] == "true" ? {"has":"Unique"} : {"!has":"Unique"});
  }
  
  var set = /s:(.+)/;
  res = RegExp(set).exec(f);
  if (res != null) {
    outp["setnumber"] = parseFloat(res[1]);
  }
  
  res = /dig(\S)([0-9])/.exec(f);
  if (res != null) {
    outp.digital = getNumberFilter(res[1],res[2]);
  }
  
  
  if ($.isEmptyObject(outp) && f != "")  {
    outp["name"] = {likenocase:f.split('|')};
  }
  
  return outp;
}

function getNumberFilter(op,val)  {
  var value = parseInt(val,10);
  var numberFilter = '';
  switch (op) {
    case ':':
      numberFilter = value;
      break;
    case '>':
      numberFilter = {">":value};
      break;
    case '<':
      numberFilter  = {"<":value};
      break;
    case '!':
      numberFilter  = {"!=":value};
      break;
  }
  return numberFilter;
}

/* Fisher-Yates Shuffle  */
function shuffle(array) {
  var m = array.length, t, i;

  // While there remain elements to shuffle…
  while (m) {

    // Pick a remaining element…
    i = Math.floor(Math.random() * m--);

    // And swap it with the current element.
    t = array[m];
    array[m] = array[i];
    array[i] = t;
  }

  return array;
}


function add_toast(msg) {
  var $toast = $('<div class="toast" role="alert" aria-live="assertive" aria-atomic="true" data-autohide="true">'
    + '<div class="toast-header"><i class="fas fa-exclamation text-primary mr-2"></i><b class="mr-auto">AoSC DB</b></div>'
    + '<div class="toast-body">'
    + msg
    + '</div></div>');
  $('#toaster').append($toast);
  $toast.toast({delay: 3000}).toast("show");
}