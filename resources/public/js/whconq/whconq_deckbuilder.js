/* Deckbuilder Script */

var _cards;
var decklist = TAFFY();

var p_fac = "";
var filter_base = {"type_code":"army_unit"};
var filter_base_type = ["warlord_unit","synapse_unit","army_unit","attachment","event","support"];
var filter_factions = [{},{}];
var filter_custom = {};

var orders = 'type asec, name asec';

var _planets;
var _sets;
var _cycles;
var _factions;

var LS_pre = 'whk-pack-';    // Local Storage Prefix

var typeicon = {
  warlord_unit: '<i class="fas fa-user-circle"></i>',
  army_unit:    '<i class="fas fa-users"></i>',
  attachment:   '<i class="fas fa-user-plus"></i>',
  event:        '<i class="fas fa-bolt"></i>',
  support:      '<i class="fas fa-hands-helping"></i>',
  synapse_unit: '<i class="fas fa-dna"></i>'
}

// Markdown
var converter = new showdown.Converter()

/* INITIALISATION */
$.getJSON("/whconq/api/cards", function (data) { 
  _cards = TAFFY(data);
  // update blank \ missing values for sorting
  _cards({"command_icons":""}).update("command_icons",0);
  _cards().each(function (card) { // pre-populate zeroes.... data to be fixed
    $.each(["cost","command_icons","shields","attack","hp"], function (id,field)  {
      if (typeof card[field] === 'undefined') {
        _cards({"code":card.code}).update(field,0);
      }
    });
  });
  
  $.getJSON("/whconq/api/factions", function (data) { _factions = TAFFY(data);
    $.getJSON("/whconq/api/cycles", function (data) { _cycles = TAFFY(data);
      $.getJSON("/whconq/api/packs", function (data) { _sets = TAFFY(data);
        _planets = JSON.parse(_cards({"type_code":"planet"}).stringify());
                  
        writeSets();
        loadDeck();
        writeDeck();
        
        updateTableBody();
      });
    });
  });
});

// typeahead 
$('#filterlist')
  .typeahead({
    hint: true,
    highlight: true,
    minLength: 2
  },
  {
    name: 'cardnames',
    source: function findMatches(q,cb) {
              cb(_cards({"name":{"likenocase":q},
                         "pack_code":filter_base.pack_code},
                         [{"faction_code":p_fac,
                           "signature_loyal":{"!=":"Signature"}},
                          {"faction_code":$.merge(["neutral"],_factions({"code":p_fac}).first().ally_codes),
                           "signature_loyal":{"isNull":true}}]).select("name"));}
  })
  .on('typeahead:select typeahead:autocomplete', function(ev, suggestion) {
    var trigger = $(this);
    $('#cardmodal').on('hidden.bs.modal',function() {
      setTimeout(function () {
        trigger.typeahead('val','').trigger('input').focus();
      },100);
    });
    var card = _cards({"name":suggestion}).first();
    setcardmodal(card)
    $('#cardmodal').modal('show');
  }); 

function loadDeck() {
  if ($.parseJSON($('#deck-content').val() != "")) {
    var deck = $.parseJSON($('#deck-content').val());
    $.each(deck, function(key, val) {
      card = _cards({"code":key}).first();
      card.qty = val;
      decklist.insert(card);
    });
  }
  $('#tags').val ($('#deck-tags').val())
  $('#notes').val ($('#deck-notes').val())
}

/* Decklist Listeners*/
$(document).on('click','.card-tooltip',function (e) {
  e.preventDefault();
  var card = _cards({"code":String($(this).data("code"))}).first();
  setcardmodal(card);
});
function setcardmodal(card) {
  var outp = '';
  var maxallowed = maxindeck(card);
  var inset = decklist({"code":card.code}).count() != 0 ? decklist({"code":card.code}).first().qty : 0;
  
  var btns = '<div class="btn-group btn-group-toggle mx-2" data-toggle="buttons">';
  if (card.signature_loyal == "Signature")  {
    btns += '<label class="btn btn-md btn-info">'
          + '<input type="radio">' + inset + '</label>';
  } else {
    for (var i=0; i<=maxallowed; i++) {
      btns += '<label class="btn btn-md btn-outline-secondary' + (i==inset?' active':'') + '">'
      + '<input type="radio" name="qty-' + card.code + '" value="' + i + '">' + i + '</label>';
    }
  }
  btns += '</div>';    
  
  outp = '<div class="modal-dialog">'
          + '<div class="modal-content">'
            + '<div class="modal-header justify-content-between">'
              + '<h4 class="modal-title">' + card.name  + '</h4>'
              + btns
              + '<button type="button" class="close" data-dismiss="modal" aria-label="Close"><span>x</span>'
            + '</div>'
            + '<div class="modal-body">'
            + '<div class="row"><div class="col"><img src="' + card.img + '"></div>'
            + '</div>'
          + '</div>'
        + '</div>';
  
  $('#cardmodal').html (outp);
}

$('#cardmodal')
  .on('change','input[type=radio]',function () {
    updateDeck(this.name.substring(4), parseInt($(this).val(),10));
    $('#cardmodal').modal('hide');
  })
  .on('keypress',function (ev)  {
    var num = parseInt(ev.which, 10) - 48;
    $('.modal input[type=radio][value=' + num + ']').trigger('change');
  });



/* BUILD TAB */

$('#tablebody').on('change','input[type=radio]:enabled',function() {
  updateDeck(this.name.substring(4), parseInt($(this).val(),10));
});

$('#factionfilter').on('change', function() {
  var facs = [];
  $.each($(this).find('input:checked'), function (k,v) {
    facs.push(v.name);
  });
  update_filter_factions(facs);
  updateTableBody();
});  

$('#filterlist').on('input', function () {
  filter_custom = parsefilter($(this).val());
  updateTableBody()
});

$('#typefilter').on('change', function() {
  var typs = [];
  $.each($(this).find('input:checked'), function (k, v) {
    typs.push(v.name);
  });
  filter_base.type_code = (typs.length == 0 ? filter_base_type : typs);
  updateTableBody();
});

$('thead').on('click', '.sortable', function () {
  var field = $(this).data('field');
  switch(orders)  {
    case field + ' desc': orders = field + ' asec'; break;
    case field + ' asec': orders = ''; break;
    default: orders = field + ' desc';
  }
  $.each(this.parentNode.getElementsByClassName("caret"), function (id, node) {
    node.remove();
  });
  if (orders != "") {
    $(this).append('<span class="caret"><i class="fas fa-caret-' + (orders.slice(-4) == "asec" ? 'up' : 'down') + '"></span>');
  }
  updateTableBody();
});

// Deck Format  
function updateDeck(cardcode, cardqty)  {
  var card = _cards({"code":cardcode}).first();
  card["qty"] = cardqty;
  
  
  switch (card.type_code) {
    case ("warlord_unit"):
      decklist({"type_code":"warlord_unit"}).remove();
      decklist({"signature_loyal":"Signature"}).remove();
      if (card.qty>0) {
          // Add Signature Cards
          _cards({"signature_squad":card.signature_squad}).each (function (sig) {
            sig.qty = sig.quantity;
            decklist.insert(sig);
          });
        };
      break;
    default:
      decklist({"code":card.code}).remove();
      if (card.qty>0) {decklist.insert(card);}
  }
  
  writeDeck();
  updateTableBody();
}

function updateTableBody() {
// Build Filter, clear and re-write 

  var results = _cards(filter_base,filter_factions).filter(filter_custom).order(orders + ", name");

  $('#tablebody').empty();   
  results.each(function(r) {
    $('#tablebody').append (buildRow(r));
  });
}
function buildRow (r) {
/* Row: House, Name, Cost, Strength */
  var maxallowed = maxindeck(r);
  var inset = decklist({"code":r.code}).count() != 0 ? decklist({"code":r.code}).first().qty : 0;
  
  var vns = _cards({"name":r.name}).count();
  
  var btns = '<div class="btn-group whk-btn-group-xs btn-group-toggle" data-toggle="buttons">';
  if (r.signature_loyal == "Signature") {
     btns += '<label class="btn btn-sm btn-info" disabled>'
            + '<input type="radio" name="qty-' + r.code + '" value="' + i + '" disabled>' + inset 
            + '</label>';
  } else {
    for (var i=0; i<=maxallowed; i++) {
      btns += '<label class="btn btn-outline-secondary' + (i==inset?' active':'') + '">'
            + '<input type="radio" name="qty-' + r.code + '" value="' + i + '">' + i 
            + '</label>';
    }
  }
  btns += '</div>';
  
  outp = '<tr>'
    + '<td>' + btns
    + '<td>'
      + '<a href="/card/' + r.code + '" class="card-tooltip" data-code="' + r.code + '" data-toggle="modal" data-target="#cardmodal" data-remote="false">' 
      + (r.unique ? '&bull;&nbsp;' : '') 
      + r.name
      + (vns > 1 ? '&nbsp;(' + r.Set + ')' : '')
      + (r.signature_loyal == "Loyal" ? '<i class="fas fa-crosshairs icon-loyal ml-2" title="Loyal"></i>' : '')
      + '</a>'
    + '</td>'
    + '<td class="text-center">' + (typeof r.type !== "undefined" ? '<span title="' + r.type + '">' + typeicon[r.type_code] + '</span>' : "") + '</td>' //'<span title="' + r.type + '">' + typeicon[r.type_code] + '</span>'
    + '<td class="text-center">' + r.faction.slice(0,2) + '</td>'
    + '<td class="text-center">' + (typeof r.cost !== "undefined" ? r.cost : "") + '</td>'
    + '<td class="text-center">' + (typeof r.command_icons !== "undefined" ? r.command_icons : "") + '</td>'
    + '<td class="text-center">' + (typeof r.shields !== "undefined" ? r.shields : "") + '</td>'
    + '<td class="text-center">' + (typeof r.attack !== "undefined" ? r.attack : "") + '</td>'
    + '<td class="text-center">' + (typeof r.hp !== "undefined" ? r.hp : "") + '</td>'
    + '</tr>';
  return (outp);
}
function maxindeck(r) {
  var core = localStorage.getItem( LS_pre + 'core-count') == null ? 1 : localStorage.getItem( LS_pre + 'core-count') ;
  var maxallowed = 3;
  
  switch (r.type_code) {
    case ("warlord_unit"):
      maxallowed = 1;
      break;
    default:
      if (r.signature_loyal == "signature")  {
        maxallowed = r.quantity;
      }
      maxallowed = (r.pack_code == "core" ? Math.min(r.quantity * core, maxallowed) : maxallowed);
  }
  return maxallowed;
}

// Write Deklist, update charts and sample draw
function writeDeck()  {
  var outp = '';
  var faction; 
  var faction_code;
  
  var identity = decklist({"type_code":"warlord_unit"}).first();
  faction_code = typeof identity == 'undefined' ? 'neutral' : identity.faction_code;
  if ((typeof faction_code !== 'undefined') && faction_code != p_fac) {  // Initial setup or Warlord has changed!
    set_primary_faction(faction_code);
  }
  
  var deckTypes = ["Army Unit","Synapse Unit","Attachment","Event","Support"];
  var numcards = decklist({"type_code":{"!is":"warlord_unit"}}).sum("qty");
  
  if (decklist().count() == 0) {
    outp = '<div class="row h5">Empty Deck</div>';
  } else {
    // Header
    if (identity)  {
      outp += '<div class="row">'
            + '<div class="col-sm-4">'
              + '<img class="rounded img-fluid d-none d-sm-block" src="' + identity.img + '"></img>'
            + '</div>'
            + '<div class="col-sm-8">'
              + '<div><a href="/card/' + identity.code + '" class="card-tooltip h4" data-code="' + identity.code + '">'
              + identity.name + '</a></div>'
              + '<div class="h5 text-muted">' 
              + /^<b>(.+)<\/b><br>/.exec(identity.text)[1]
              + '</div>'
            // Check Validity
              + (numcards < 50 ? '<div class="h5 text-danger">' : '<div class="h5">' ) + 'Cards: ' + numcards +'/50' + '</div>'
              + '<div class="small">' + checkValidity() + '</div>'
            + '</div></div>';
    }

    // Content
    outp+= '<div class="decklist">'
    $.each(deckTypes, function (id,cardtype) {
      if (decklist({"type":cardtype}).count() > 0) {
        outp += '<div class="decklist-section"><b>' + cardtype + ' (' + decklist({"type":cardtype}).sum("qty") + ')</b>';
        decklist({"type":cardtype}).order("name").each(function (card) {
          outp += '<br>' + card.qty + 'x <a href="/card/' + card.code + '" class="card-tooltip" data-code="' + card.code + '" data-toggle="modal" data-remote="false" data-target="#cardmodal">' + card.name + '</a>';
          if (card.faction_code != faction_code) { outp += ' <i class="fa fa-flag ' + card.faction_code + '"></i>'; }
          if (card.signature_loyal == "Signature") { outp += ' <i class="fa fa-cog icon-sig"></i>'; }
        });
        outp+='</div>';
      }
    });
  }    
  $('#decklist').html(outp);
  
  updateCharts();
  
  // update deck-content for saving and newGame
  var deck_data = {};
  decklist().each(function(card) {
    deck_data[card.code] = card.qty;
  });
  $('#deck-content').val(JSON.stringify(deck_data));    
  newGame();
}
function checkValidity()  {
  // Returns html formatted string
  var validresult = [];
  //1 deck size - Included in Standard output
  if (decklist({"type_code":{"!is":"warlord_unit"}}).count() < 50)  {validresult.push ("Minimum deck size is 50.");}
  
  var faction_code = decklist({"type_code":"warlord_unit"}).first().faction_code;
  
  if (typeof faction_code !== 'undefined')  {
  //2 cards from forbidden factions
    var faction_codes = decklist({"faction_code":{"!is":"neutral"}}).distinct("faction_code");
    var ally_codes = $.merge([faction_code], _factions({"code":faction_code}).first().ally_codes);
    var not_allies = [];
    
    $.each(faction_codes, function (id, ally)  {
      if ($.inArray(ally, ally_codes) < 0) {not_allies.push(_factions({"code":ally}).first().name);}
    });
    if (not_allies.length > 0) {
      validresult.push ('Cannot include cards from unallied factions: <span class="text-danger">' + not_allies.join(', ') + '</span>')
    } else {
  //3 cards from multiple factions - change for NECRONS?
      var factions = decklist({"faction_code":{"!is":"neutral"}}).distinct("faction");
      if (factions.length > 2) {
        validresult.push ('Only inlcude cards from one allied faction: <span class="text-danger">' + factions.join(', ') + '</span>');
      }
    }
  }
  
  //4 loyal cards from other factions
  var sigloyal = [];
  decklist({"faction_code":{"!is":faction_code},"signature_loyal":["Signature","Loyal"]}).each(function (r) {
      sigloyal.push ('<a href="/card/' + r.code + '" data-code="' + r.code + '" class="card-tooltip" data-toggle="modal" data-target="#cardmodal">' + r.name + '</a>');
  });
  if (sigloyal.length > 0) {
    validresult.push ('Cannot include Loyal cards from other factions: <span class="text-danger">' + sigloyal.join(', ') + '</span>');
  }
  return (validresult.length > 0 ? validresult.join('<br />') : "Deck is Valid");
}

// filter code
function set_primary_faction(fac) {
  var a_fac = [];
  p_fac = fac;
  a_fac = $.merge(["neutral",p_fac],_factions({"code":p_fac}).first().ally_codes);
  
  // reset faction_selection
  $('#factionfilter').find('input:checked').prop('checked',false);
  $('#factionfilter').find('label').removeClass('active');
  
  $.each($('#factionfilter').find('input'),function (id, ele) {
    if (a_fac.indexOf(ele.name) > -1) {
      $(ele.parentNode).removeClass('disabled');
      $(ele).removeAttr('disabled');
    } else {
      $(ele.parentNode).addClass('disabled');
      $(ele).attr('disabled',true);
    }
  });
  
  $('#deck-alliance').val(p_fac);
  update_filter_factions(a_fac);
}

// build faction filter  
function update_filter_factions(facs) {
  var i = facs.indexOf(p_fac);
  
  if (i < 0)  { //Primary faction NOT included in filter
    if (facs.length == 0) {
      if (p_fac == "") {
        filter_factions = {};
      } else {
        update_filter_factions($.merge(["neutral",p_fac],_factions({"code":p_fac}).first().ally_codes));
      }
    } else {
       filter_factions = [{"faction_code":facs,"signature_loyal":{"isNull":true}}];
    }
  } else {      //Primary faction IS included in filter
    facs.splice(i,1);
    // NOTE: "faction_code":[] returns no records, which is good. Alt is to not include the filter - faster? more readable?
    if (facs.length == 0) {
      filter_factions = [{"faction_code":p_fac,"signature_loyal":{"!=":"Signature"}}];
    } else {
      filter_factions = [{"faction_code":p_fac,"signature_loyal":{"!=":"Signature"}},{"faction_code":facs,"signature_loyal":{"isNull":true}}];
    }
  }
  
  updateTableBody();
}

/* NOTES TAB */
$('#tags').on('change',function () {
  $('#deck-tags').val ($(this).val());
});
$('#notes').on('keyup',function () {
  $('#deck-notes').val ($(this).val());
  $('#notes-preview').html (converter.makeHtml($(this).val()));
});

/* SETS TAB */
// SETS: Json {Set: {code: code: number: number}}
// <div id="setlist"></div>

$('#setlist')
  .on('change','input[type=radio]',function() { // Core Sets
    localStorage.setItem($(this).attr('name'),$(this).val());
    updateSetFilter();
    updateTableBody();
  })
  .on('click','input.pack',function () { // Pack
    localStorage.setItem("whk-pack-" + $(this).data('code'),$(this).prop('checked').toString() );
    updateSetFilter();
    updateTableBody();
  })
// Parent/child checkboxes
  .on("click","div.form-check.pa",function()  {
    var packcode;
    var checkchild = $(this).find("input[type='checkbox']").prop("checked");
    var cyclecode = $(this).find("input[type='checkbox']").data("cycle");
    $(this).parent().find("[data-cycle='" + cyclecode + "']input[type='checkbox'].pack").each(function (id,chkbox)  {
      $(chkbox).prop("checked",checkchild);
      packcode = $(chkbox).data('code');
      localStorage.setItem("whk-pack-" + packcode,checkchild);
    });
    updateSetFilter();
    updateTableBody();
  })
  .on("click","div.form-check.ch",function()  {
    var checkpar = $(this).parent().find("input[type='checkbox']:checked").length == $(this).parent().find("input[type='checkbox']").length;
    var cyclecode = $(this).find("input[type='checkbox']").data("cycle");
    $(this).parent().parent().find("div.form-check.pa [data-cycle='" + cyclecode + "']input[type='checkbox']").prop("checked",checkpar);
    // updateSetFilter(); triggered by .on('click','input.pack',function ()
  });
  

function writeSets() {
  var outp = '';
  var core = localStorage.getItem(LS_pre + 'core-count') == null ? 1 : localStorage.getItem(LS_pre + 'core-count');
  
  _cycles().order("position").each( function (cycle) {
    indent = false;
    cyclepacks = _sets({"cycle_code":cycle.code}).select("code");
    _sets({"cycle_code":cycle.code}).order("position").each( function (pack,idx) {
      if (pack.name == "Core Set") {
        outp += '<div class="my-2"><span class="mr-2">Core Sets</span>'
              + '<div class="btn-group btn-group-toggle" data-toggle="buttons">';
        for (var i=1; i<4; i++) {
          outp += '<label class="btn btn-sm btn-light' + (i==core?' active':'') + '">'
                + '<input type="radio" name="whk-pack-core-count" value="' + i + '">' + i 
                + '</label>';
        }
        outp += '</div></div>';
      } else {
                    
        if (cycle.name != pack.name && idx == 0) {
          indent = true;
          outp += '<div class="form-check pa">'
            + '<input class="form-check-input cycle" type="checkbox" data-cycle="' + pack.cycle_code + '" checked="checked">'
            + '<label class="form-check-label"><b>'
            + cycle.name
            + '</b></label></div>';
            outp += '<ul>';
        }
        outp += '<div class="form-check ch">'
          + '<input class="form-check-input pack" type="checkbox" data-code="' + pack.code + '" data-cycle="' + pack.cycle_code + '" checked="checked">'
          + '<label class="form-check-label">'
          + (cycle.name == pack.name ? '<b>' : '') 
          + pack.name 
          + (cycle.name == pack.name ? '</b>' : '')
          + ' (' + _cards({"pack_code":pack.code}).sum("quantity") + ')'
          + '</label></div>';
      }
    });
    if (indent == true)  {
      outp += '</ul>';
    }
  });
  $('#setlist').html (outp);
  
  // Set PACKS Checkboxes
  if (typeof(Storage) !== "undefined")  {
    _sets({"code":{"!in":"core"}}).order("position").each( function (pack,idx) {
      // get localstorage value
      checked = localStorage.getItem("whk-pack-" + pack.code) == null ? true : localStorage.getItem("whk-pack-" + pack.code) == "true";
      // un-check child and parent
      if (checked == false)  {
        $('#setlist').find('input[data-code="' + pack.code + '"]').prop("checked",checked);
        $('#setlist').find('input.cycle[data-cycle="' + pack.cycle_code + '"]').prop("checked",checked);
      }
    });
  }
  updateSetFilter();
}

function updateSetFilter() {
  /* build set filter */
  var setFilter = [];
  _sets().each( function(set) {
    if (set.code == 'core') {
      setFilter.push ('core');
    } else {
      if (localStorage.getItem(LS_pre + set.code) == "true" || localStorage.getItem(LS_pre + set.code) === null) {
        setFilter.push (set.code)
      }
    }
  });
  filter_base.pack_code = setFilter;
}
  
/* CHECK TAB */

$('.btn-draw').on('click',function() {
  if ($(this).attr('val') == 0) {
    newGame();
  } else {
    var n = $(this).attr('val') == "all" ? deck.length : $(this).attr('val');
    drawCards(n);
  }
  $('#draw1').prop('disabled',deck.length == 0);
  $('#drawall').prop('disabled',deck.length == 0);
  $('#draw2').prop('disabled',deck.length < 2);
  $('#draw7').prop('disabled',deck.length < 7);
});
$('#hand').on('click','.check_card',function() {
// Make cards 50% opaque when clicked
  $(this).css('opacity', 1.5 - parseFloat($(this).css('opacity')));
})

function newGame() {
  _deck = [];
  var warlord = decklist({"type_code":"warlord_unit"}).first()
  var starting_hand = (typeof warlord === 'undefined' ? 0 : warlord.starting_hand);
  
  decklist({"type_code":{"!is":"warlord_unit"}}).each(function(card) {
    for (i=0; i<card.qty; i++) {
      _deck.push({"code":card.code,"img":card.img,"name":card.name});
    }
  });
  _deck = shuffle(_deck);
  deck = _deck.slice();
  
  $('#hand').html ('');
  drawCards(starting_hand);
  drawPlanets();
}

function drawCards(n) {
  var card;
  n = Math.min(n, deck.length);
  for (var i = 0; i < n; i++) {
    card = deck.shift();
    $('#hand').append('<img src="' + card.img + '" class="check_card deck_card" data-code="' + card.code + '"></img>');
  }
}
function drawPlanets()  {
  var outp = '';
  _cards({"type_code":"planet"}).each( function (r) {
    outp += '<img src = "' + r.img + '" class="deck_card_planet" data-code="' + r.code + '"></img>';
  });
  $('#planets').html (outp);
}

// Code to compile data and call Charts scripts

// Pie - faction distribution
// Pie - Shield Icons

// Line - Cost

// define charts
var chartColours = {
  "Space Marines": "goldenrod",
  "Astra Militarum": "darkslategrey",
  "Orks": "darkgreen",
  "Chaos": "darkorange",
  "Dark Eldar": "purple",
  "Eldar": "gold",
  "Tau": "lightskyblue",
  "Tyrranids": "brown",
  "Necrons": "teal",
  "Neutral": "darkgrey",
  "Army Unit": "goldenrod",
  "Attachment": "lightskyblue",
  "Support": "lightgray",
  "Event": "thistle"
};

var chart_pf = new Chart (
  $('#pieFact').get(0).getContext("2d"),
  {
    type: 'pie',
    data: {
      labels: [], 
      datasets: [{
         label: "", 
         data: [], 
         backgroundColor: []}]},
    options: {
      legend: {
        position: 'bottom'
      },
      title: {
        display: true,
        text: "Cards per faction"
      },
      plugins: {
        labels: {
          render: 'value'
        }
      }
    }
  }
);

var chart_pt = new Chart (
  $('#pieType').get(0).getContext("2d"),
  {
    type: 'pie',
    data: {
      labels: [], 
      datasets: [{
         label: "", 
         data: [], 
         backgroundColor: []}]},
    options: {
      legend: {
        position: 'bottom'
      },
      title: {
        display: true,
        text: "Cards per type"
      },
      plugins: {
        labels: {
          render: 'value'
        }
      }
    }
  }
);

var chart_ps = new Chart (
  $('#pieShield').get(0).getContext("2d"),
  {
    type: 'pie',
    data: {
      labels: [], 
      datasets: [{
         label: "", 
         data: [], 
         backgroundColor: []}]},
    options: {
      legend: {
        display: false,
        position: 'bottom'
      },
      title: {
        display: true,
        text: "Shields per card"
      },
      plugins: {
        labels: {
          render: 'value'
        }
      }
    }
  }
);

var chart_pc = new Chart (
  $('#pieCommand').get(0).getContext("2d"),
  {
    type: 'pie',
    data: {
      labels: [], 
      datasets: [{
         label: "", 
         data: [], 
         backgroundColor: []}]},
    options: {
      legend: {
        display: false,
        position: 'bottom'
      },
      title: {
        display: true,
        text: "Command Icons per card"
      },
      plugins: {
        labels: {
          render: 'value'
        }
      }
    }
  }
);

// Line Cost
var chart_lc = new Chart (
  $('#lineCost').get(0).getContext("2d"),
  {
    type: 'line',
    data: {
      labels: [],
      datasets: [{
        label: "", data: []
    }]},
    options: {
      legend: {
        position: 'top'
      },
      title: {
        display: true,
        text: "# Cards by Cost/Attack/HP",
        fontSize: 18,
        fontStyle: 'bold'
      },
      scales: {
        yAxes: [{
          scaleLabel: {
            labelString: "# Cards",
            display: true
          },
          ticks: {
            beginAtZero: true,
            stepSize: 1
          }
        }]
      }
    }
  }
);
  
function getChartData_pf () {
    var data = {
      labels: [], 
      datasets: [{
         label: "", 
         data: [], 
         backgroundColor: []}]}
    data.datasets[0].label = "Faction";
    $.each(decklist().distinct("faction"), function (id, title) {
      data.labels.push(title);
      data.datasets[0].data.push(decklist({"faction" : title, "type_code":{"!=":"warlord_unit"}}).sum("qty"));
      data.datasets[0].backgroundColor.push(chartColours[title]);
    });
    return data;
};

function getChartData_pt () {
    var data = {
      labels: [], 
      datasets: [{
         label: "", 
         data: [], 
         backgroundColor: []}]}
    data.datasets[0].label = "Type";
    $.each(decklist({"type_code":{"!=":"warlord_unit"}}).distinct("type"), function (id, title) {
      data.labels.push(title);
      data.datasets[0].data.push(decklist({"type" : title}).sum("qty"));
      data.datasets[0].backgroundColor.push(chartColours[title]);
    });
    return data;
};

function getChartData_ps () {
    var data = {
      labels: [], 
      datasets: [{
         label: "", 
         data: [], 
         backgroundColor: []}]}
    data.datasets[0].label = "Shields";
    var clr;
    /*
    data.labels.push('0 Shields')
    data.datasets[0].data.push(
      decklist({"type_code":{"!=":"warlord_unit"}}).sum("qty") - 
      decklist({"type_code":{"!=":"warlord_unit"},"shields":{isNumber:true}}).sum("qty"));
    data.datasets[0].backgroundColor.push('rgb(240,240,256)');
    */
    for (var i = 0; i < 4; i++) {
      data.labels.push(i + ' Shields');
      data.datasets[0].data.push(decklist({"shields" : i}).sum("qty"));
      clr = 256 - (i * 32);
      data.datasets[0].backgroundColor.push('rgb(' + clr + ', ' + clr + ', ' + 256 + ')');
    };
    return data;
};

function getChartData_pc () {
    var data = {
      labels: [], 
      datasets: [{
         label: "", 
         data: [], 
         backgroundColor: []}]}
    data.datasets[0].label = "Command Icons";
    var clr;
    /*
    data.labels.push('0 Cmd')
    data.datasets[0].data.push(
      decklist({"type_code":{"!=":"warlord_unit"}}).sum("qty") - 
      decklist({"type_code":{"!=":"warlord_unit"},"command_icons":{isNumber:true}}).sum("qty"));
    data.datasets[0].backgroundColor.push('rgb(256,240,240)');
    */
    var maxcommand = decklist({"type_code":{"!=":"warlord_unit"}}).max("command_icons");
    for (var i = 0; i < maxcommand + 1; i++) {
      data.labels.push(i + ' Cmd');
      data.datasets[0].data.push(decklist({"type_code":{"!=":"warlord_unit"},"command_icons" : i}).sum("qty"));
      clr = 256 - (i * 32);
      data.datasets[0].backgroundColor.push('rgb(' + 256 + ', ' + clr + ', ' + clr + ')');
    };
    return data;
};

function getChartData_lc () {
  var maxval = Math.max(
    decklist({"type_code":{"!=":"warlord_unit"}}).max("cost"),
    decklist({"type_code":{"!=":"warlord_unit"}}).max("attack"),
    decklist({"type_code":{"!=":"warlord_unit"}}).max("hp")
    );
  var data = {
    labels: [], 
    datasets: [{
      label: 'Cost', data: [], 
      lineTension: 0,
      fill: false, borderColor: 'blue'
    },{
      label: 'Attack', data: [], 
      lineTension: 0,
      fill: false, borderColor: 'red'
    },{
      label: 'HP', data: [], 
      lineTension: 0,
      fill: false, borderColor: 'green'
    }]
  };
  for (var i = 0; i <= maxval; i++)  {
    data.labels.push(i);
    data.datasets[0].data.push(decklist({"type_code":{"!=":"warlord_unit"},"cost":i}).sum("qty"));
    data.datasets[1].data.push(decklist({"type_code":"army_unit","attack":i}).sum("qty"));
    data.datasets[2].data.push(decklist({"type_code":"army_unit","hp":i}).sum("qty"));
  };
  return data;
}

function updateChart(chart, chartData) {
  chart.data = chartData;
  chart.update();
}

function updateCharts()  {
  updateChart(chart_pf, getChartData_pf()); //Faction
  updateChart(chart_pt, getChartData_pt()); //Type
  updateChart(chart_ps, getChartData_ps()); //Shields
  updateChart(chart_pc, getChartData_pc()); //Command Icons
  
  updateChart(chart_lc, getChartData_lc()); //Cost
}


// Form Validation
(function() {
  'use strict';
  window.addEventListener('load', function() {
    // Fetch all the forms we want to apply custom Bootstrap validation styles to
    var forms = document.getElementsByClassName('needs-validation');
    // Loop over them and prevent submission
    var validation = Array.prototype.filter.call(forms, function(form) {
      form.addEventListener('submit', function(event) {
        if (form.checkValidity() === false) {
          event.preventDefault();
          event.stopPropagation();
        }
        form.classList.add('was-validated');
      }, false);
    });
  }, false);
})();  
