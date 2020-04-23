var _db_cards;
var _db_packs;
var _filter = {"pack_code":["Core"],"type_code":["hero","ally","attachment","event"]};  //treasure, player-side-quest 'objective ally'
var _filter_custom = {};
var _corecount = 1;
var _sort = "name asec";
var _deck = {};

var converter = new showdown.Converter();

$.getJSON('/lotrdb/api/data/cards',function (data) {
  data = data.filter(c => -1 < $.inArray(c.type_code, _filter.type_code));
  data = addzeroes(data,["attack","defense","willpower","threat"])
  data = data.map(c => (c.threat != -1 ? $.extend({cost: c.threat},c) : c)); 
  _db_cards = data;
  
  $.getJSON('/lotrdb/api/data/packs',function (data) {
    _db_packs = TAFFY(data);
  // extend cards to include cycle code
    _db_cards = TAFFY(_db_cards.map(c=>$.extend({"cycle_position": _db_packs({"code":c.pack_code}).first().cycle_position},c)));
    _deck = ($('#deckdata').val() != "" ? JSON.parse($('#deckdata').val()) : {});
    // Check Local Storage
    var cc = localStorage.getItem('lotrcore_owned');
    if (cc) { 
      _corecount = parseInt(cc,10);
      $('#' + _corecount + 'core').closest('label').button('toggle');
    }
    var pcks = localStorage.getItem('lotrpacks_owned');
    if (pcks) {
      _filter.pack_code = ["Core"].concat(JSON.parse(pcks));
      _filter.pack_code.map(function (p) {
        $('#collectiontab').find('input[data-id=' + p + ']').prop('checked',true);
      });
      $('#collectiontab li').each(function (id, e) {
        $(e).find('input[data-type=cycle]').prop('checked',($(e).find('input[data-type=pack]:checked').length == 6));
      });
    }
    write_table();
    write_deck();
  });
});

function write_table () {
  var cardtbl = $('#cardtbl')
  cardtbl.empty();
  _db_cards(_filter).filter(_filter_custom).order(_sort).each(c => cardtbl.append(tblrow(c)));
}

function tblrow (c) {
  return '<tr>'
    + '<td>' + deck_buttons(c) + '</td>'
    + '<td><a class="card-link" data-toggle="modal" data-target="#cardmodal" href="/lotrdb/card/' + c.code + '" data-code="' + c.code + '">' 
      + (c.is_unique ? '&bull;&nbsp;' : '')
      + c.name 
      + (_db_cards({"name":c.name}).count()>1 ? ' (' + c.pack_code +')' : '')
      + '</a></td>'
    + '<td class="text-center">' + c.type_name + '</td>'
    + '<td class="text-center" title="' + c.sphere_name + '"><img class="icon-xs" src="/img/lotrdb/icons/sphere_' + c.sphere_code + '.png"</img></td>'
    + '<td class="text-center">' + (c.threat != -1 ? c.threat : (c.cost != -1 ? c.cost : "-"))+ '</td>'
    + '<td class="text-center">' + (c.attack != -1 ? c.attack : "-")+ '</td>'
    + '<td class="text-center">' + (c.defense != -1 ? c.defense : "-")+ '</td>'
    + '<td class="text-center">' + (c.willpower != -1 ? c.willpower : "-")+ '</td>'
    + '</tr>';
}  

function deck_buttons (c) {
  var maxindeck = maxAllowedInDeck(c);
  var indeck = (typeof _deck[c.code] == 'undefined' ? 0 : _deck[c.code]);
  var outp = '';
  
  outp += '<div class="btn-group btn-group-xs btn-group-toggle" data-toggle="buttons">'
  for (i=0; i<=maxindeck; i++) {
    outp += '<label class="btn btn-outline-dark' 
      + (i == indeck ? ' active' : '')
      + '">'
      + '<input type="radio" name="' + c.code + '" val=' + i + '>' + i + '</input></label>';
  }
  return outp;
}

function maxAllowedInDeck (c) {
  return Math.min(c.deck_limit, c.quantity * (c.pack_code == "Core" ? _corecount : 1))
}

function addzeroes (data, fields) {
  var z = [];
  $.each(fields, function(k,v) {
    z[v]=-1
    data = data.map(c => $.extend({}, z, c));
  });
  return data;
}


function write_deck () {
  var deckcards = _db_cards({"code": Object.keys(_deck)}).map(c=>$.extend({"qty": _deck[c.code]},c))
  var outp = '';
  outp = '<div class="d-flex mb-2">';
  $.each(deckcards.filter(c=>c.type_code == "hero"),function (i, c) {
    outp += '<a class="card-link" '
      + 'href="lotrdb/card/' + c.code + '" '
      + 'data-code="' + c.code + '" data-toggle="modal" data-target="#cardmodal">'
      + '<div class="deckhero" '
      + 'style = "background-image: url(' + c.cgdbimgurl + '); position: relative;">'
      + '<span style="position: absolute; right: 2px; bottom: 2px;">'
      + '<image src="/img/lotrdb/icons/sphere_' + c.sphere_code + '.png" style="width: 35px;" />'
      + '</span>'
      + '</div></a>';
  });
  outp+='<div class="p-3">' + deckcards.filter(c=>c.type_code != "hero").reduce((t,c)=>t+=parseInt(c.qty),0) + '/50 cards</div>';
  outp+='</div>';
  outp += '<div style="-webkit-column-gap: 20px; -webkit-column-count: 2; -moz-column-gap: 20px; -moz-column-count: 2; column-gap: 20px; column-count: 2;">';
  $.each(["Ally","Attachment","Event"],function (n, t) {
    var cardsOfType = deckcards.filter(c=>c.type_name == t);
    if (cardsOfType.length > 0) {
      outp += '<div style="break-inside: avoid;"><span class="font-weight-bold">' + t + ' (' + cardsOfType.reduce((t,c)=>t+=parseInt(c.qty),0) + ')</span>';
      $.each(deckcards.filter(c=>c.type_name == t), function (i, c) {
        outp += '<div>' + c.qty + 'x <a class="card-link" data-toggle="modal" data-target="#cardmodal" data-code="' + c.code + '" href="/lotrdb/card/' + c.code + '">' + c.name + '</a></div>';
      });
      outp+='</div>';
    }
  });
  outp+='</div>';
    
  $('#decklist').html(outp);
}

// Filters //
function update_filter (k, v) {
  if (v.length == 0) { delete _filter[k]; } 
  else { _filter[k] = v; }
  write_table();
}
  
$('.btn-group').on('change',function() {
  update_filter (this.id, $(this).find('input:checked').map(function () { return $(this).data('code')}).get())
});

// Sorting //

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
  var qty = parseInt($(this).attr('val'));
  var code = $(this).attr('name');
  updateDeckCount(code, qty);
});
function updateDeckCount(code, qty) {
  if (qty == 0) {
    delete _deck[code]
  } else {
    _deck[code] = qty
  }
  $('#deckdata').val(JSON.stringify(_deck));
  write_deck();
}

// Modal
$('body').on('click','.card-link',function (e) {
  e.preventDefault();
});

$('#cardmodal')
  .on('show.bs.modal', function (event) {
    var code = $(event.relatedTarget).data("code").toString(); // Button that triggered the modal
    var crd = _db_cards({"code":code}).first();
    if (typeof crd.code !== 'undefined') {setModalHtml($(this),crd);}
  })
  .on('hidden.bs.modal', function () {
    $(this).find('.modal-header').html('');
    $(this).find('.modal-body').html('');
  })
  .on('change','input[type=radio]',function () {
    updateDeckCount($(this).data("code"), $(this).attr("value"));
    $('#cardmodal').modal('hide');
  })
  .on('keypress',function (ev)  {
    var num = parseInt(ev.which, 10) - 48;
    $('.modal input[type=radio][value=' + num + ']').trigger('change');
  });;

function setModalHtml(modal,crd) {
  modal
    .find('.modal-header')
    .html('<div class="modal-title"><h4 class="mr-2 align-middle">' 
      + ($.inArray("Unique",crd.tags) != -1 ? '&bull;&nbsp;' : '')
      + crd.name 
      + '</h4>'
      + modalButtonGroup(crd)
      + '</div>'
      + '<button class="close" type="button" data-dismiss="modal"><span>&times;</span></button>');
  modal
    .find('.modal-body')
    .html('<img src="' + crd.cgdbimgurl + '" class="img-fluid" />');
  modal
    .find('.modal-footer')
    .html('<span>' + crd.pack_name + ' #' + crd.position + '</span>');
}  
function modalButtonGroup(crd) {
  var count = (_deck[crd.code] || 0);
  var outp =  '<div class="btn-group btn-group-toggle" data-toggle="buttons">';
  for (i=0; i<=maxAllowedInDeck(crd); i++) {
    outp += '<label class="btn'
      + (crd.owned < i ? ' btn-outline-danger' : ' btn-outline-secondary')
      + (count == i ? ' active' : '')
      + '">'
      + '<input type="radio" value="' + i + '" name="' + crd.id + '" data-code="' + crd.code + '">'
      + i
      + '</label>';
  }
  outp += '</div>';
  return outp;
}

// SEARCH

$('#filtertext').on('input', function () {
  _filter_custom = parsefilter($(this).val());
  write_table()
});

/***** TYPEAHEAD *****/

$('#filtertext').typeahead({
  hint: true, highlight: true, minLength: 2
},{
  name: 'lotrcardnames',
  source: 
    function findMatches (q, cb) {
      cb(_db_cards(_filter).filter({"name":{"likenocase":q}}).select("name"));}
})
//.on('typeahead:select typeahead:autocomplete input',function () {
//  val = $(this).val();
//  delete _filter.name;
//  if (val != "") { _filter.name = {"likenocase":val} }
//  write_table();
//});




// NOTES TAB

$('#tags').on('input',function() {
  $('#decktags').val($(this).val());
});
$('#notes').on('input',function() {
  $('#notesmarkdown').html( converter.makeHtml($(this).val()) );
  $('#decknotes').val($(this).val());
})



// COLLECTION TAB

$('#collectiontab')
  .on('change','input[type=radio]', function () {
    _corecount = $(this).val();
    window.localStorage.setItem('lotrcore_owned',_corecount);
    write_table();  
  })
  .on('change','input[type=checkbox]', function() {
    var $li = $(this).closest('li');
    if ($(this).data('type') == "cycle") {
      $li.find('input[data-type=pack]').prop('checked',$(this).is(':checked'));
    } else {
      $li.find('input[data-type=cycle]').prop('checked', ($li.find('input[data-type=pack]:checked').length > 5));    
    }
    var x = Array.from($('#collectiontab').find('input[data-type=pack][type=checkbox]:checked')).map(e=>$(e).data('id'));
    _filter.pack_code = ["Core"].concat(x);
    window.localStorage.setItem('lotrpacks_owned',JSON.stringify(_filter.pack_code));
    write_table();
  });