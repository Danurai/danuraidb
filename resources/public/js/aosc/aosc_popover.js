const icon_path = '/img/aosc/icons/';


var is_touch_device = ("ontouchstart" in window) || window.DocumentTouch && document instanceof DocumentTouch;

if (! is_touch_device) {
  function setpopover(ele, crd) {
    $(ele).popover({
      trigger: 'hover',
      placement: 'auto',
      html: true,
      content: tooltip_body(crd)
    }).popover('show');
  }
}

function tooltip_corners(crn,cat) {
  var outp = '';
  var fname = '';
  $.each(crn, function (id, c) {
    outp += '<span class="popover-corner-bg popover-corner-' + cat.toLowerCase() + (c.smooth ? "smooth" : "clunky") + '">';
    if (c.value != "o") {
      if ($.isNumeric(c.value) || c.value == "X") {
        outp += '<span class="popover-corner-value">' + c.value + '</span>';
      } else {
        fname = 'quest_' + c.value.toLowerCase() + (typeof c.qualifier !== 'undefined' ? '_' + c.qualifier.toLowerCase() : '' ) + '.png';
        outp += '<img src="' + icon_path + fname +'" class="popover-corner-image"></img>';
      }
    }
    outp += '</span>';
  });
  return outp;
}

function tooltip_body(crd)  {
  var cs = class_style(crd);
  var subjfname = 'subject_' + crd.subjectImage + '.png';
  return '<h5>'
    + ($.inArray("Unique",crd.tags) != -1 ? '&bull;&nbsp;' : '')
    + crd.name + '</h5>'
    + '<div class="px-1 pb-1">'
    + taglist(crd.tags)
    + markdown(crd.effect[_lang]) 
    + '</div>'
    + '<div class="d-flex pb-1">' + tooltip_corners(crd.corners, crd.category.en) + '</div>'
    + (typeof crd.subjectImage !== "undefined" 
        ? '<div class="text-center"><img class="mx-auto subject-icon" src="' + icon_path + subjfname + '"></img></div>' 
        : '')
    + '<div class="d-flex mt-2">'
    + '<div class="bl-' + cs[0] + ' bg-' + cs[0] + '"></div>'
    + '<div class="br-' + cs[1] + ' bg-' + cs[1] + '"></div>'
    + '</div>'
    + (crd.errata.length > 0 ? crd.errata.map(c=>'<div>' + c.errata + '</div>').join("") : '');
}
function class_style(crd) {
  var cs;
  if (typeof crd.class === 'undefined') {
    switch (crd.category.en) {
      case "Unit":
        cs = ["war","war"];
        break;case 
      "Spell":
        cs = ["wiz","wiz"];
        break;
      default:
        cs = ["any","any"];
    }
  } else {
    if (crd.class.en == "Warrior Wizard") {
      cs = ["war", "wiz"];
    } else {
      cs = [crd.class.en.slice(0,3).toLowerCase(), crd.class.en.slice(0,3).toLowerCase()];
    }
  }
  return cs;
}

function taglist(tags) {
  var outp = '';
  if (typeof tags !== 'undefined') {
    outp = '<div class="px-1"><i>' + tags.join(" ") + '</i></div>';
  }
  return outp;
}

function markdown(str)  {
  var rtn;
  rtn = str.replace(/(\[(.+?)\])/g,'<span class="px-1 popover-keyword">$2</span>');
  rtn = rtn.replace(/(\*(.+?)\*)/g, 
    function (x) {
      var patt = new RegExp(/\*(.+?)\*/);
      patt.exec(x);
      return '<em>' + RegExp.$1 + '</em><span class="popover-icon"><span class="aosc-type-' + RegExp.$1.toLowerCase() + '"></span></span>';
    });
  rtn = rtn.replace(/[X]/g, '<b>X</b>');
  
  return rtn;
}

$('.search-info').popover({
  trigger: 'hover',
  placement: 'auto',
  html: true,
  content: '<b>Search hints</b><span class="small">'
    + '<br>&lt;name&gt;<br>a: alliance'
    + '<br>c: category'
    + '<br>w: class <i>or</i> &quot;class&quot;'
    + '<br>t: tag(|tag)'
    + '<br>s:!<> set number'
    + '<br>r: rarity'
    + '<br>o:!<> cost'
    + '<br>h:!<> health modifier'
    + '<br>x: text <i>or</i> &quot;text&quot;'
    + '<br>u: unique (true|false)'
});