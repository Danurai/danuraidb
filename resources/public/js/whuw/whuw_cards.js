var _filter = {};
var _whuw_cards=TAFFY();
const WHUWCARDPATH = "/img/whuw/cards/";
// WHUWCARDPATH + card.filname OR card.url

$.fn.selectpicker.Constructor.DEFAULTS.multipleSeparator = " ";


$.getJSON('/whuw/api/cards', function (d) {
  _whuw_cards=TAFFY(d);

  var savedsets = localStorage.getItem("whuwsets");
  if (savedsets == "" || savedsets == null) {
    savedsets = [156];
  }

  _filter.set_id = JSON.parse(savedsets);
  $('#selectset').selectpicker('val',_filter.set_id);
  
  write_results();
  
  function write_results () {
    $('#results').empty();
    $cards = $('<div class="row">');
    $('#results').append('<small class="col-sm-12 mb-1">Cards returned: ' + _whuw_cards(_filter).count() + '</small>');
    $.get(_whuw_cards(_filter).first().url, function () {
      _whuw_cards(_filter)
        .order("card_type_id, name")
        .each(c=>$cards.append(cardimg (c, c.url)));
    })
    .fail (function () {
      _whuw_cards(_filter)
        .order("card_type_id, name")
        .each(c=>$cards.append(cardimg (c, WHUWCARDPATH + c.filename)));
    });
      
    $('#results').append($cards);
  }
  
  function cardimg ( c, src ) {
    var style = 'img-fluid' +
      (c.banned ? ' card-forsaken' :
        (c.restricted ? ' card-restricted' : ''))
      + (!c.championship_legal ? ' card-illegal' : '')
    var restrictions = [];
    var title = '' ;
    
    if (c.banned) {restrictions.push("Forsaken")};
    if (c.restricted) {restrictions.push("Restricted 3/deck")};
    if (!c.championship_legal) {restrictions.push("Not Championship Legal")};
    title = c.name + (restrictions.length > 0 ? ' (' + restrictions.join(', ') + ')' : '')
    
    return '<div class="col-sm-3 mb-2">'
      + '<img class="' + style + '" src="' + src + '" title="' + title + '" alt="' + c.filename + '"></img>'
      + '</div>';
  }
  
  
  $('#selectset').on('change', function () { update_filter("set_id",$(this).val()); });
  $('#selectwarband').on('change', function () { update_filter("warband_id",$(this).val()); });
  $('#selecttype').on('change', function () { update_filter("card_type_id",$(this).val()); });
  
  function update_filter ( fld, vals ) {
    delete _filter[fld];
    if (vals.length > 0) {
      _filter[fld] = vals.map(v=>parseInt(v));
    }
    write_results();
  }
});