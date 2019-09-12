var _cards;
const icon_unique = '<i class="lotr-type-unique unique-icon mr-1"></i>'; 

function lortdb_markdown (str) {
  var rtn;
  // Icons
  rtn = str.replace(/\[(\w+)\]/g,(($0,$1) => '<i class="lotr-type-' + $1.toLowerCase() + '"></i>'));
  return rtn;
}


$.getJSON("/lotrdb/api/data/cards", function (data) {
  _cards = TAFFY(data);
  
  function card_icon(c) {
    var img = '';
    img = '<img class="icon-sm float-right" src="/img/lotrdb/icons/'
    switch (c.type_code) {
      case 'hero':
      case 'attachment':
      case 'ally':
      case 'event':
        img += 'sphere_' + c.sphere_code;
        break;
      default:
        img += 'pack_' + c.pack_code;
    }
    img += '.png"></img>'
    return img
  }
  
  $('body').on('mouseover','.card-link',function () {
    crd = _cards({"code":$(this).data('code').toString()}).first();
    $(this).popover({
      trigger: 'hover',
      placement: 'auto',
      html: true,
      title: 
        '<span class="h4 clearfix">' 
        + (crd.is_unique ? icon_unique : '')
        + crd.name 
        + card_icon(crd)
        + '</span>',
      content: 					
        (typeof crd.traits !== 'undefined' ? '<div class="text-center mb-1"><b><em>' + crd.traits + '</em></b></div>' : '') +
        '<div style="white-space: pre-wrap;">' + lortdb_markdown(crd.text) + '</div>'
    }).popover('show');
  });
});
  

$('.search-info').popover({
  trigger: 'hover',
  placement: 'auto',
  html: true,
  content: '<b>Search hints</b><span class="small">'
    + '<br>&lt;name&gt;'
    + '<br>e: Pack Code (core, thfg etc)'      
    + '<br>n: Encounter Name (Passage Through Mirkwood)'
    + '<br>r: Traits'
    + '<br>s: Sphere code (<b>l</b>eadership l<b>o</b>re <b>t</b>actics <b>s</b>pirit'
    + '<br>t: Type code (<b>h</b>ero <b>a</b>lly <b>e</b>vent a<b>t</b>tachment)'
    + '<br>x: Card Text'
    + '<br>y:!<> Cycle number'
    + '<br>d: deck (<b>p</b>layer <b>e</b>nemy <b>q</b>uest)'
    + '<br>u:true|false Unique'
});