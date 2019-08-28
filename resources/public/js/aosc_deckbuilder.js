var _cards;
var _collection;
var wavecounts = {};
var _filter = {};
var _filter_custom = {};
var _sort = "id asec";
var _decklist = [];
var _owned = false;
var _lang = "en";

var converter = new showdown.Converter();


$.getJSON('/aosc/api/data/cards',function (data) {
  var source_data = data['hits']['hits'].map(c => c['_source']);
  _cards = TAFFY(
    source_data.map(card => $.extend({"setnumber": card.set[0].number,"setname": card.set[0].name,"text": card.effect.en, },card))
  );
  
  $.getJSON('/aosc/api/private/collection', function(data) {
    _collection = data;
    $.each(_collection, function (k, v) {
      _collection[k].total = (typeof v.digital !== 'undefined' ? parseInt(v.digital,10) : 0) + (typeof v.physical !== 'undefined' ? parseInt(v.physical,10) : 0)
    });
    
    try {
      _decklist = parse($('#deckdata').val()).cards;
      $.each(_decklist, function (id, item) {
        update_alliance(item.id);
      });
    } catch (e) {console.log (e)}
    _filter.alliance = [$('#filter_alliance').find("input[type=radio]:checked").prop("id"),"Any"];
    
    write_table();
    write_deck();
    $('#usernotes').trigger('input');
  });
});

function write_table () {
  var crd;
  $('#tblbody').empty();
  _cards(_filter).filter(_filter_custom).order(_sort).each(function (c)  {
    crd = $.extend({"digital":0,"physical":0,"foil":0},_collection[c.id],c);
    crd.owned = parseInt(crd.digital,10) + parseInt(crd.physical,10) + parseInt(crd.foil,10);
    $('#tblbody').append(table_row (crd));
  });
}
function table_row (c)  {
  var hiderow = $('#lock').prop('checked');
  return '<tr' 
      + (c.owned == 0 ? ' class="table-secondary text-muted' + (hiderow ? ' hiderow' : '') + '"' : '') 
      + '>'
    + '<td>' + card_button_group(c) + '</td>'
    + '<td><span data-toggle="modal" data-target="#cardmodal" data-id="'+  c.id + '" class="' + getCardClass(c) +'">'
      + '<a href="#" data-id = "' + c.id + '" class="card-tooltip" data-toggle="popover">'
      + ($.inArray("Unique",c.tags) != -1 ? '&bull;&nbsp;' : '')
      + c.name + '</span></span></td>'
    + '<td><img class="trait-icon" src="/img/aosc/icons/category_' + c.category.en.toLowerCase() + '.png"></td>'
    + '<td class="text-center">' 
      + trait_icons(c.tags, true)
      + '</td>'
    + '<td class="text-center">' + (typeof c.healthMod !== 'undefined' ? '<span class="hp">' + c.healthMod + '</span>': '') + '</td>'
    + '<td class="text-center">' + (typeof c.cost !== 'undefined' ? '<span class="cost">' +  c.cost + '</span>': '') + '</td>';
}   
function card_button_group(c) {
  var outp = '<div class="btn-group btn-group-toggle btn-group-xs" data-toggle="buttons">';
  var deckdata = _decklist.filter(function(c) {return c.id == this}, c.id);
  var count = (deckdata.length > 0 ? deckdata[0].count : 0);
  var max = maxAllowedInDeck(c);
  for (var x=0; x<=Math.min(3, max); x++) { //limit to 3+ for table layout
    outp += '<label class="btn '
      + (c.owned < x ? "btn-outline-danger" : "btn-outline-secondary")
      + (x == count ? ' active' : '') + '">'
      + '<input type="radio" name="' + c.id + '" val=' + x + ' data-id = ' + c.id + '>'
      + x 
      + '</label>';
  }
  if (max > 3) {outp += '<label class="btn '
      + (c.owned < 4 ? "btn-outline-danger" : "btn-outline-secondary") + (count > 3 ? ' active' : '') + '"'
      + 'data-id = ' + c.id + ' data-toggle="modal" data-target="#cardmodal">'
      + '<input type="radio" name="' + c.id + '" val=4>'
      + '+'
      + '</label>';
  }
  outp += '</div>';
  return outp;
}
function maxAllowedInDeck(c) {
  if (c._id == -117917574 || c.name == "Festering Nurglings"){   //Festering Nurglings
    return 7;
  } else if ($.inArray("Unique", c.tags) == true || c.category.en == "Blessing") {
    return 1;
  } else if (c.category.en == "Champion") {
    return 2;
  } else {
    return 3;
  }
}

function write_deck() {
  var deck = TAFFY();
  var card;
  var generated_deck = (_decklist.length != 0 ? generate(_decklist) : "")
  $('#deckdata').val(generated_deck);
  
  write_deck_cards(
      TAFFY(
      _decklist.map(
        function (c) {
           return $.extend(c,this({"id":c.id}).first())
        }, _cards)));
}

function write_deck_cards (deck)  {
  var card_types = ["Champion","Blessing","Unit","Spell","Ability"];
  var outp = '';
  var cost = 0;
  var health = 30;
  deck().each(function (c) {
    cost += (typeof c.cost !== 'undefined' ? c.cost * c.count : 0)
    health += (typeof c.healthMod !== 'undefined' ? c.healthMod * c.count : 0)
  });
// Header
  var actions = deck({"category":{"en":["Unit","Ability","Spell"]}}).sum('count');
  var uniqueChampions = deck({"category":{"en":"Champion"},"tags":{"has":"Unique"}}).count();
  outp += '<div class="row-fluid my-2">'
    + (uniqueChampions > 1 ? '<span class="text-danger mr-1">More than one Unique Champion!</span>' : '')
    + 'Cards: <span class="' + (actions != 30 ? 'text-danger' : 'text-success') + '">' + actions + '/30  </span>'
    + 'Cost: <span class="' + (cost > 20 ? 'text-danger' : 'text-success') + '">' + cost + "/20  </span>"
    + 'Health: ' + (Math.min (health, 35))
    + '</div>';
// Body
  outp += '<div class="row mb-1"><div class="col-lg-6">'
    + write_section(deck, "Champion")
    + '</div><div class="col-lg-6">'
    + write_section(deck,"Blessing")
    + '</div></div>';
  outp += '<div class="row"><div class="col-lg-6">'
    + (deck({"category":{"en":"Unit"}}).count() > 0 ? write_section(deck, "Unit") : '')
    + '</div><div class="col-lg-6">'
    + (deck({"category":{"en":"Spell"}}).count() > 0 ? write_section(deck, "Spell") : '')
    + (deck({"category":{"en":"Ability"}}).count() > 0 ? write_section(deck, "Ability") : '')
    + '</div></div>';
  $('#decklist').html(outp);
}
function write_section (deck, type) {
  var outp = '';
  var card_cls = '';
  outp += '<b>' + type + '</b> (' + deck({"category":{"en":type}}).sum('count') + ')';
  deck({"category":{"en":type}}).order("name asec").each(function (c) {
    card_cls = getCardClass(c)
    
    outp += '<div class="' + card_cls + '">'      
      + '<span data-toggle="modal" data-target="#cardmodal" data-id="'+  c.id + '">'
      + '<a href="/card/' + c.id + '" data-id = "' + c.id + '" class="card-tooltip" data-toggle="popover">'
      + c.count + 'x ' 
      + c.name 
      + ($.inArray("Unique",c.tags) != -1 ? '&nbsp;(U)' : '')
      + '</a></span>'
      + trait_icons(c.tags, false)
      + '</div>';
  });
  return outp;
}
function getCardClass (c) {
  if (typeof c.class !== "undefined") {
    switch (c.class["en"]) {
      case "Warrior":
      case "Wizard":
      case "Any":
        return "class-" + c.class["en"].toLowerCase();
        break;
      case "Warrior Wizard":
        return "class-warrior-wizard";
        break;
      default:
        return "class-any";
    }
  } else {
    switch (c.category["en"]) {
      case "Unit":
        return "class-warrior";
        break;
      case "Spell":
        return "class-wizard";
        break;
      default:
        return "class-any";
    }
  }
}

function trait_icons (traits) {
  var outp = '';
  if (typeof traits !== 'undefined')  {
    $.each(traits, function (id, trait) {
      outp += '<img class="img-fluid trait-icon ml-1" src="/img/aosc/icons/tag_' + trait.toLowerCase() + '.png"></img>';
    });
  }
  return outp;
}

function trait_icons_icomoon (traits)  {
  var outp = '';
  if (typeof traits !== 'undefined')  {
    $.each(traits.filter(t => t != "Unique"), function (id, trait) {
      outp += '<span class="type-icon ml-1"><span class="aosc-type-' + trait.toLowerCase() + '" title="' + trait + '"></span></span>'; 
    });
  }
  return outp;
}

$('#filter_type').on('change',function() {
  var types = [];
  delete _filter.category;
  $(this).closest('div').find('input:checked').each(function (id, btn) {
    types.push (btn.id);
  });
  if (types.length > 0) {_filter.category = {}, _filter.category[_lang] = types}
  write_table();
});

$('#filter_alliance').on('change','input[type=radio]',function() {
  _filter.alliance = [this.id,"Any"];
  write_table();
});
$('#selecttrait').on('changed.bs.select',function() {
  delete _filter.tags;
  if ($(this).val().length > 0) {
    _filter.tags = {"has": $(this).val()};
  }
  write_table();
});

// sort
$('thead').on('click','.sortable',function() {    
  var field = $(this).data("field");
  switch(_sort)  {
    case field + ' asec': _sort = field + ' desc'; break;
    case field + ' desc': _sort = ''; break;
    default: _sort = field + ' asec';
  }
  $.each(this.parentNode.getElementsByClassName("caret"), function (id, node) {
    node.remove();
  });
  if (_sort != "") {
    $(this).append('<span class="caret"><i class="fas fa-caret-' + (_sort.slice(-4) == "asec" ? 'down' : 'up') + '"></span>');
  }
  write_table();
});

$('#lock').on('change',function () {
  $(this).parent()
    .find('[data-fa-i2svg]')
    .toggleClass("fa-lock")
    .toggleClass("fa-unlock");
  $('#tblbody').find('tr.table-secondary').toggleClass("hiderow");
});

$('body')
  .on('click','.cardlink',function (ev) {
    ev.preventDefault();
  })
  .on('mouseover','.card-tooltip',function () {
    setpopover(this, _cards({"id":$(this).data("id")}).first());
  })
  .on('click','.card-tooltip',function (ev) {
    ev.preventDefault();
  });

$('#tblbody').on('change','input[type=radio]',function() {
  if ($(this).attr("val") < 4) { // only trigger for 0-3
    updateDeckCount($(this).data("id"), $(this).attr("val"));
  } else {
    write_table();
  }
});


$('#cardmodal')
  .on('show.bs.modal', function (event) {
    var id = $(event.relatedTarget).data("id"); // Button that triggered the modal
    var crd = _cards({"id":id}).first();
    if (typeof crd.id !== 'undefined') {setModalHtml($(this),crd);}
  })
  .on('hidden.bs.modal', function () {
    $(this).find('.modal-header').html('');
    $(this).find('.modal-body').html('');
  })
  .on('change','input[type=radio]',function () {
    updateDeckCount($(this).data("id"), $(this).attr("value"));
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
      + '</h4>' + modalButtonGroup(crd)
      + '</div>'
      + '<button class="close" type="button" data-dismiss="modal"><span>&times;</span></button>');
  modal
    .find('.modal-body')
    .html('<div class="row">'
      + '<div class="col-sm-9">'
      + '<img src="/img/cards/' + crd.skus.filter(sku => (sku.default == true && sku.lang == "en"))[0].id + '.jpg" style="width: 100%;"></img>'
      + '</div></div>');
}  
function modalButtonGroup(crd) {
  crd = $.extend({"digital":0,"physical":0,"foil":0},_collection[crd.id],crd);
  crd.owned = parseInt(crd.digital,10) + parseInt(crd.physical,10) + parseInt(crd.foil,10);
  var deckdata = _decklist.filter(function(c) {return c.id == this}, crd.id);
  var count = (deckdata.length > 0 ? deckdata[0].count : 0);
  var outp = '<div>';
  outp += '<div class="btn-group btn-group-toggle" data-toggle="buttons">';
  for (i=0; i<=maxAllowedInDeck(crd); i++) {
    outp += '<label class="btn'
      + (crd.owned < i ? ' btn-outline-danger' : ' btn-outline-secondary')
      + (count == i ? ' active' : '')
      + '">'
      + '<input type="radio" value="' + i + '" name="' + crd.id + '" data-id="' + crd.id + '">'
      + i
      + '</label>';
  }
  outp += '</div></div>';
  return outp;
}

function updateDeckCount (id, count) {
  _decklist = _decklist.filter(function (c) {return c.id != this}, id);
  if (count > 0) {
    _decklist.push({"id": id, "count": parseInt(count)})
  }
  update_alliance(id);
  write_table();
  write_deck();
}

function update_alliance(id)  {
  var c = _cards({"id":id}).first();
  if (c.alliance != "Any") {$('#deckalliance').val(c.alliance)}
}

// SEARCH
$('#filtertext').on('input', function () {
  _filter_custom = parsefilter($(this).val());
  write_table()
});
  
// typeahead 
$('#filtertext')
  .typeahead({
    hint: true,
    highlight: true,
    minLength: 2
  },
  {
    name: 'cardnames',
    source: function findMatches(q,cb) {
              cb(_cards({"name":{"likenocase":q}}).select("name"));}
  })
  .on('typeahead:select typeahead:autocomplete', function(ev, suggestion) {
    var trigger = $(this);
    $('#cardmodal').on('hidden.bs.modal',function() {
      setTimeout(function () {
        trigger.typeahead('val','').trigger('input').focus();
      },100);
    });
    var card = _cards({"name":suggestion}).first();
    setModalHtml($('#cardmodal'),card)
    $(this).data("id", card.id);
    $('#cardmodal').modal('show');
  }); 


// DECK NOTES TAB
$('#usernotes').on('input',function () {
  $('#notesmarkdown').html( converter.makeHtml($(this).val()) );
  $('#decknotes').val( $(this).val() );
});


// CHECK Tab
