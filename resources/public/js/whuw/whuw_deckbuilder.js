var _cards;
var _sets;
var _warbands;
var _types;
var _data;
var _championship = $('#championstoggle').hasClass("active");

var filter = {"championship_legal":true};
var order = "name asec";
var decklist = [];
const WHUWCARDPATH = "/img/whuw/cards/";

$.fn.selectpicker.Constructor.DEFAULTS.multipleSeparator = " ";

$.getJSON('/whuw/api/cards', function (data) {
  _data = data.map(function (c) {if (typeof c.glory == "undefined") {c.glory = 0} return c});
  _cards = TAFFY(data);
  
  decklist = ($('#deck-content').val() == "" ? [] : JSON.parse($('#deck-content').val()));
  if (decklist.length != _cards({"code":decklist}).count() ) {
     decklist = decklist.map(c => _cards({"gw_id":c}).first().code);
  }
  
  // ` selected sets between sessions
  var savedsets = localStorage.getItem("whuwsets");
  if (savedsets == "" || savedsets == null) {
    savedsets=[156];
    localStorage.setItem("whuwdata",savedsets);
  }
  filter.set_id = JSON.parse(savedsets);
  $('#selectset').selectpicker('val',filter.set_id);

// Save deck in LocalStorage?

  // initialise warbands for existing decks
  var warband = $('#deck-alliance').val();
  if (warband != "") {
    filter.warband_id = [35,parseInt(warband)];
    $('#selectwarband').selectpicker('val',filter.warband_id);
  }
  set_warband_icon();
  
  write_table();
  write_deck();
});  


function set_warband_icon() {
  var warband_card = _cards({"code":decklist,"warband":{"!=":35}}).first();
  if (warband_card) {
    $('#deckicon').attr("src", "/img/whuw/icons/" + warband_card.warband_icon);
  }
}


function write_table () {
  $('#info').html('<span class="small w-100 text-right text-muted float-right">Showing ' + _cards(filter).count() + '/' + _cards().count() + ' cards</span>');
  $('#cardtbl').empty();
  _cards(filter).order(order).each(function (crd) { add_row(crd) });
}

function add_row (crd) {
  var indeck = (decklist.indexOf(crd.code) > -1);
  $('#cardtbl').append(
    '<tr' + (!crd.championship_legal ? ' style="background-color: lightslategrey"' : '') + '>'
    + '<td>'
      + ban_restrict_icon(crd)
      + '<a class="cardlink" href="#" data-code="' + crd.code + '" data-toggle="modal" data-target="#cardmodal">' 
      + '<span>' +  crd.name + '</span>'
      + '</a>'
    + '</td>'
    + '<td class="text-center">'
      + '<img class="icon-sm" src="/img/whuw/icons/' + crd.card_type_icon + '" title="' + crd.card_type_name + '">'
    + '</td>'
    + '<td class="text-center">'
      + '<img class="icon-sm" src="/img/whuw/icons/' + crd.set_icon + '" title="' + crd.set_name + '">'
    + '</td>'
    + '<td class="text-center">'
      + '<img class="icon-sm" src="/img/whuw/icons/' + crd.warband_icon + '" title="' + crd.warband_name + '">'
    + '</td>'
    + '<td class="text-center">' + (crd.glory == 0 ? "-" : crd.glory) + '</td>'
    + '<td class="text-center">'
      +'<button type="button" class="btn btn-' + (indeck ? 'danger' : 'success') + ' btn-sm decktoggle" data-code="' + crd.code + '">'
      + (indeck ? '-' : '+')
      + '</button></td>'
    + '</tr>'
  )
}

function ban_restrict_icon (c) {
  return (c.banned ? '<i class="mr-1 text-danger fa-sm fas fa-times-circle" title="Forsaken"></i>' :
    (c.restricted ? '<i class="mr-1 text-info fa-sm fas fa-exclamation-circle" title="Restricted (3 per deck)"></i>' : ''))
}

function validity () {
  var obj = _cards({"code":decklist,"card_type_id":20}).count();
  var ploy = _cards({"code":decklist,"card_type_id":[21,150]}).count();
  var ug = _cards({"code":decklist,"card_type_id":22}).count();
  var ban = _cards({"code":decklist,"banned":true}).count();
  var restrict = _cards({"code":decklist,"restricted":true}).count();
  var rotated = _cards({"code":decklist.filter(code=>code.substring(0,2)=="01"),"warband_name":"Universal"}).count();
  
  valid = (obj == 12 && (ploy + ug) > 19 && ploy <= ((ploy + ug) / 2) && ban == 0 && restrict <= 3 && rotated ==0)
  
  return '<span class="mr-2 font-weight-bold">Validity:</span>' 
    + (valid 
      ? '<span class="text-success"><i class="fas fa-check mr-2"></i>' 
      : '<span class="text-danger"><i class="fas fa-times mr-2"></i>')
    + '<span class="mr-1">Objectives: ' + obj + '/12</span>'
    + '<span class="mr-1">Power: ' + (ploy + ug) + '>=20</span>'
    + '<span class="mr-1">Ploys: ' + ploy + '<=' + ((ploy + ug) / 2) + '</span>'
    + '<span class="mr-1">Forsaken: ' + ban + '</span>'
    + '<span class="mr-1">Restricted: ' + restrict + '/3</span>'
    + '<span class="mr-1">Rotated: ' + rotated + '</span>'
    + '</span>';
}

function isRotated (c) {
  return (c.code.substring(0,2) == "01" && c.warband_name == "Universal")
}

function write_deck () {
  var outp = '<div class="col">';
  var deck_groups = [
    {card_types: 20, name: "Objectives"},
    {card_types: 21, name: "Ploys"},
    {card_types: 150, name: "Gambit Spells"},
    {card_types: 22, name: "Upgrades"}];
  outp += '<div class="row">' + validity() + '</div>';
  outp += '<div class="row"><div class="decklist">';
  $.each(deck_groups, function (key, type) {
    if (_cards({"code":decklist,"card_type_id":type.card_types}).count() > 0) {
      outp += '<div class="decklist-section mb-1"><b>' 
        + type.name 
        + ' (' + _cards({"code":decklist,"card_type_id":type.card_types}).count() + ')'
        + '</b>'
        + (type.name == "Objectives"
          ? '&nbsp<span class="text-muted">' 
            + _cards({"code":decklist,"card_type_id":type.card_types})
              .select("glory").map(c=>parseInt(c))
              .reduce((t,c)=>t+=c,0) 
            + 'pts</span>'
          : '');
        _cards({"code":decklist,"card_type_id":type.card_types}).order("name asec").each(function (c) {
          outp += '<div ' + (isRotated (c) ? 'style="text-decoration: line-through" title="Rotated"' : "") + '>'
            + ban_restrict_icon (c)
            + '<a href="#" class="cardlink mr-1" data-code="' + c.code + '" data-toggle="modal" data-target="#cardmodal">' 
            + c.name
            + '</a>'
            + '<span class="text-muted">' + (c.glory > 0 ? c.glory : '') + '</span>'
            + '</div>';
      });
      outp += '</div>';
    }
  });
  outp += '</div></div></div>';
  $('#decklist').html(outp);
}

$('body').on('click','.decktoggle',function (evt)  {
  var code = $(this).data("code");
  var dl_idx = decklist.indexOf(code);
  if (dl_idx == -1) {
    decklist.push(code);
  } else {
    decklist.splice(dl_idx, 1);
  }
// Set warband Filter if unset
  var warband_card = _cards({"code":decklist,"warband":{"!=":35}}).first();
  if (warband_card && $('#deck-alliance').val() != warband_card.warband) {
    $('#deck-alliance').val(warband_card.warband);
    $('#selectwarband').selectpicker('val',[35,warband_card.warband]).trigger('change');
    $('#deckicon').attr("src", "/img/whuw/icons/" + warband_card.warband_icon);
  }
  $('#deck-content').val(JSON.stringify(decklist));
  write_deck();
  write_table();
});

var img = $('<img></img>');
var _modal_card;

$('#cardmodal').on('show.bs.modal',function (evt) {
  var crd = _cards({"code": ($(evt.relatedTarget).data("code"))}).first();
  var indeck = (decklist.indexOf(crd.code) > -1);
  var $body = $(this).find('.modal-body');
  var addbutton = '<button type="button" class="btn btn-secondary btn-sm float-right decktoggle" data-code="' + crd.code + '" data-dismiss="modal">'
      + (indeck ? 'Remove' : 'Add')
      + '</button>';
  var img = $('<img class="img-fluid mb-2"></img>'); 
  
  img.on('error',function () {
    $(img).attr('src',WHUWCARDPATH + crd.filename);
  });
  img.attr('src',crd.url);
      
  $(this).find('.modal-title').html(ban_restrict_icon (crd) + crd.name);
  
  $body.append(ban_restrict_info(crd));
  $body.append(img);
  $body.append(addbutton);
  
});


function ban_restrict_info (c) {
  return (c.banned ? '<div class="text-danger text-small">This card is Forsaken</div>' : '')
    + (c.restricted ? '<div class="text-info text-small">This card is restricted - limit 3 restricted cards per deck</div>' : '')
    + (!c.championship_legal ? '<div class="text-secondary text-small">This card is not Championship Legal</div>' : '')
}

$('body')
  .on('click','.cardlink',function (evt) {
    evt.preventDefault();
  })
  .on('mouseover','.cardlink',function () {
    crd = _cards({"code":$(this).data("code")}).first();
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

iconmap = {
  "Hammer": "ra ra-battered-axe",
  "Sword":  "ra ra-crossed-swords",
  "Channel": "ra ra-lightning-trio",
  "Focus": "ra ra-slash-ring"
}
  
/***** FILTERS *****/
$('#selectwarband').on('change', function() {
  vals = $(this).val();
  delete filter.warband_id;
  if (vals.length > 0) {
    filter.warband_id = vals.map(wb => parseInt(wb));
    var swb = vals.filter(c=>c!="35");
    if (swb.length > 0 && $('#deck-alliance').val() == "") {
      $('#deck-alliance').val(swb[0]);
    }
  }
  write_table();
});

$('#selectset').on('change', function() {
  vals = $(this).val();
  delete filter.set_id;
  localStorage.removeItem("whuwsets")
  if (vals.length > 0) {
    filter.set_id = vals.map(wb => parseInt(wb));
    localStorage.setItem("whuwsets",JSON.stringify(filter.set_id));
  }
  write_table();
});

$('#selecttype').on('change', function() {
  vals = $(this).val();
  delete filter.card_type_id;
  if (vals.length > 0) {
    filter.card_type_id = vals.map(wb => parseInt(wb));
  }
  write_table();
});

$('#championstoggle').on('click', function () {
  if ($(this).hasClass('active')) {
    delete filter.championship_legal;
  } else {
    filter.championship_legal = true;
  }
  write_table();
});

/***** TYPEAHEAD *****/

$('#filtertext').typeahead({
  hint: true, highlight: true, minLength: 2
},{
  name: 'cardnames',
  source: 
    function findMatches (q, cb) {
      cb(_cards($.extend(filter,{"name":{"likenocase":q}})).select("name"));}
})
.on('typeahead:select typeahead:autocomplete input',function () {
  val = $(this).val();
  delete filter.name;
  if (val != "") { filter.name = {"likenocase":val} }
  write_table();
});

/***** SORTING *****/
$('.sortable').on('click',function () {
  $(this).closest('tr').find('.caret').remove();
  var sorting = order.split(" ");
  var field = $(this).data("field");
  var dir = "asec";
  
  if (sorting[0] = field) {  //switch direction
    dir = (sorting[1] == "asec" ? "desc" : "asec");
  }
  order = field + " " + dir;
  $(this).append('<span class="caret float-right"><i class="fas fa-sort-' + (dir == "asec" ? "up" : "down" ) + '" /></span>');
  write_table();
});

// Don't save an empty deck!
$('#save_form').on('submit',function(ev) {
  if (decklist.length == 0) {
    ev.preventDefault()
}});

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



