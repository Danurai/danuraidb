let _popcards;

$.getJSON('/whuw/api/cards',function (data) {
  _popcards = TAFFY(data);
});

$('#cardmodal').on('show.bs.modal',function (evt) {
  var crd = _popcards({"code": ($(evt.relatedTarget).data("code"))}).first();
  var indeck = false; //(decklist.indexOf(crd.code) > -1);
  var $body = $(this).find('.modal-body');
  var addbutton = '<button type="button" class="btn btn-secondary btn-sm float-right decktoggle" data-code="' + crd.code + '" data-dismiss="modal">'
      + (indeck ? 'Remove' : 'Add')
      + '</button>';
  var img = $('<img class="img-fluid mb-2"></img>'); 
    
  let WHUWCARDPATH = '/whuw/img/cards';
  $.get(WHUWCARDPATH + crd.filename,function () {
    img.attr('src',WHUWCARDPATH + crd.filename)
  }).fail(function () {
    img.attr('src',crd.url)
  });
      
  $(this).find('.modal-title').html(ban_restrict_icon (crd) + crd.name);
  
  $body.empty();
  $body.append(ban_restrict_info(crd));
  $body.append(img);
  $body.append(addbutton);
  
});

$('body')
  .on('click','.cardlink',function (evt) {
    evt.preventDefault();
  })
  .on('mouseover','.cardlink',function () {
    crd = _popcards({"code":$(this).data("code")}).first();
    $(this).popover({
      trigger: 'hover',
      placement: 'auto',
      html: true,
      title: '<img class="icon-sm float-right" src="/img/whuw/icons/' + crd.card_type_icon + '"></img>'
        + '<div>' + crd.name + '</div>',
      content: 
        '<div>' 
        + ban_restrict_info(crd)
        + '</div>'
        + '<div>' + whu_md(crd.rule) + '</div>'
        + (crd.card_type == 20 ? '<div>Glory: ' + crd.glory + '</div>' : '')
        + (crd.target != "-" ? '<div class="d-flex mt-2 justify-content-center"><span class="attack-bar">' + crd.target + '</span></div>' : '')
    }).popover('show');
  });

function whu_md (txt) {
  var pattern = /\[Hex\s([0-9])\s(\w+)\s([0-9\-])\sDmg\s([0-9])\](|\s)/
  res = pattern.exec(txt)
  if (res != null) {
    atk = '<div class="d-flex justify-content-center"><span class="attack-bar">'
      + '<span class="mr-2"><span class="mr-1">&#x2B22;</span>' + RegExp.$1 + '</span>'
      + '<span class="mr-2"><i class="mr-1 dice-icon ' + iconmap[RegExp.$2] + '"></i>' + RegExp.$3 + '</span>'
      + '<span><i class="mr-1 ra ra-bomb-explosion"></i>' + RegExp.$4 + '</span>'
      + '</span></div>';
    return txt.replace(pattern,atk);      
  } else {
    return txt;
  }
}

function ban_restrict_info (c) {
  return (c.banned ? '<div class="text-danger text-small">This card is Forsaken</div>' : '')
    + (c.restricted ? '<div class="text-info text-small">This card is restricted - limit 3 restricted cards per deck</div>' : '')
    + (!c.championship_legal ? '<div class="text-secondary text-small">This card is not Championship Legal</div>' : '')
}

function ban_restrict_icon (c) {
  return (c.banned ? '<i class="mr-1 text-danger fa-sm fas fa-times-circle" title="Forsaken"></i>' :
    (c.restricted ? '<i class="mr-1 text-info fa-sm fas fa-exclamation-circle" title="Restricted (3 per deck)"></i>' : ''))
}

iconmap = {
  "Hammer": "ra ra-battered-axe",
  "Sword":  "ra ra-crossed-swords",
  "Channel": "ra ra-lightning-trio",
  "Focus": "ra ra-slash-ring"
}
  