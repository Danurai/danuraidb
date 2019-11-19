var _filter = {};
var _whuw_cards=TAFFY();
const WHUWCARDPATH = "/img/whuw/cards/";

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
    _whuw_cards(_filter)
      .order("card_type_id, name")
      .each(c=>$cards.append(cardimg (c)));
      
    $('#results').append($cards);
  }
  
  function cardimg (c) {
    return '<div class="col-sm-3 mb-2"><img class="img-fluid" src="' + WHUWCARDPATH + c.filename + '" title="' + c.name + '" alt="' + c.filename + '"></img></div>';
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