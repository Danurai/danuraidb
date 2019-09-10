var _cards;

$.getJSON("/lotrdb/api/data/cards", function (data) {
  _cards = TAFFY(data);
  
  function card_icon(c) {
    var img = '';
    img = '<img class="icon-sm" src="/img/lotrdb/icons/'
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
        '<span class="h4">' + crd.name + '</span>'
        + '<span class="float-right">'
        + card_icon(crd)
        + '</span>',
      content: 					
        (typeof crd.traits !== 'undefined' ? '<div><b>' + crd.traits + '</b></div>' : '') +
        '<div style="white-space: pre-wrap;">' + crd.text + '</div>'
    }).popover('show');
  });
});
  