var _db_cards;
var _filter = {"pack_code":"Core","type_code":["hero","ally","attachment","event"]};  //treasure, player-side-quest 'objective ally'
var _sort = "code asec";
var _deck = {};

$.getJSON('/lotrdb/api/data/cards',function (data) {
  data = data.filter(c => -1 < $.inArray(c.type_code, _filter.type_code));
  data = addzeroes(data,["attack","defense","willpower","threat"])
  data = data.map(c => (c.threat != -1 ? $.extend({cost: c.threat},c) : c)); 
  _db_cards = TAFFY(data);
  write_table();
});

function write_table () {
  var cardtbl = $('#cardtbl')
  cardtbl.empty();
  _db_cards(_filter).order(_sort).each(c => cardtbl.append(tblrow(c)));
}

function tblrow (c) {
  return '<tr>'
    + '<td>' + deck_buttons(c) + '</td>'
    + '<td><a class="card-link" href="/card/' + c.code + '" data-code="' + c.code + '">' 
      + (c.is_unique ? '&bull;&nbsp;' : '')
      + c.name + '</a></td>'
    + '<td class="text-center">' + c.type_name + '</td>'
    + '<td class="text-center" title="' + c.sphere_name + '"><img class="icon-xs" src="/img/lotrdb/icons/sphere_' + c.sphere_code + '.png"</img></td>'
    + '<td class="text-center">' + (c.threat != -1 ? c.threat : (c.cost != -1 ? c.cost : "-"))+ '</td>'
    + '<td class="text-center">' + (c.attack != -1 ? c.attack : "-")+ '</td>'
    + '<td class="text-center">' + (c.defense != -1 ? c.defense : "-")+ '</td>'
    + '<td class="text-center">' + (c.willpower != -1 ? c.willpower : "-")+ '</td>'
    + '</tr>';
}  

function deck_buttons (c) {
  var max = (c.type_code == "hero" ? 1 : 3);
  var indeck = (typeof _deck[c.code] == 'undefined' ? 0 : _deck[c.code]);
  var outp = '';
  outp += '<div class="btn-group btn-group-xs btn-group-toggle" data-toggle="buttons">'
  for (i=0; i<=max; i++) {
    outp += '<label class="btn btn-outline-dark' 
      + (i == indeck ? ' active' : '')
      + '">'
      + '<input type="radio" name="' + c.code + '" val=' + i + '>' + i + '</input></label>';
  }
  return outp;
}
  

function addzeroes (data, fields) {
  var z = [];
  $.each(fields, function(k,v) {
    z[v]=-1
    data = data.map(c => $.extend({}, z, c));
  });
  return data;
}


// FILTERS //
function update_filter (k, v) {
  if (v.length == 0) { delete _filter[k]; } 
  else { _filter[k] = v; }
  write_table();
}
  
$('.btn-group').on('change',function() {
  update_filter (this.id, $(this).find('input:checked').map(function () { return $(this).data('code')}).get())
});

// SORTING //

$('.sortable').on('click', function () {
  var f = $(this).data('field')
  var dir = "asec"
  
  if (_sort.slice(0,f.length) == f) {
    dir = (_sort.slice(-4) == dir ? "desc" : "asec")
  }
  _sort = f + " " + dir;
  write_table();
});


// Deckbuilder

$('#cardtbl').on('change','input[type=radio]',function () {
  var qty = $(this).attr('val');
  var code = $(this).attr('name');
  if (qty == 0) {
    delete _deck[code]
  } else {
    _deck[code] = qty
  }
  // write_deck
  $('#decklist').html(JSON.stringify(_deck));
});