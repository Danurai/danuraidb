var _warbands;
var _cardtypes;
var _sets;
var _cards;
var _filter = {};
const WHUWICONPATH = "/img/whuw/icons/";
const WHUWCARDPATH = "/img/whuw/cards/";

  
$.getJSON("/whuw/api/data", function (data) {
   _warbands = TAFFY(data.warbands);
   _cardtypes = TAFFY(data["card-types"]);
   _sets = TAFFY(data["sets"]);
   
   $('#selectset').selectpicker('val',[156]);
   _filter.set_id=[156];
   
   write_warbands ();
   write_sets ();
   write_cardtypes ();
  $.getJSON("/whuw/api/cards", function (data) {
    _cards = TAFFY(data.map(c => $.extend(c, 
      {"set_icon": _sets({"id":c.set}).first().icon.filename, 
       "warband_icon": _warbands({"id":c.warband}).first().icon.filename})));
    write_cards();
  });
});

$.getJSON("/whuw/api/champions", function (data) {
  let champs = data.champions;
  let thead = $('#champions').find("thead");
  let tbody = $('#champions').find("tbody");
  
  let blocked = false;
  
  thead.html('<tr><th>id</th><th>warband_id</th><th>name</th><th>leader</th><th>img</th><th>img_ins</th></tr>');
  tbody.empty();
  //$.get(champs[0].cards[0].url).fail(c=>blocked=true);
  
  $.each(champs,function (id,ch) {
    tbody.append('<tr>'
      + '<td>' + ch.id + '</td>'
      + '<td>' + ch.warband_id + '</td>'
      + '<td>' + ch.name + '</td>'
      + '<td>' + (ch.leader ? '<i class="fas fa-crown"></i>' : '-') + '</td>'
      + '<td>' + (blocked ? 'X' : '<img src="' + ch.cards[0].url + '" style="width: 100px;"></img>') + '</td>'
      + '<td>' + (blocked ? 'X' : '<img src="' + ch.cards[1].url + '" style="width: 100px;"></img>') + '</td>'
      + '</tr>')
  });
});


function write_warbands() {
  var thead = $('#warbands').find("thead");
  var tbody = $('#warbands').find("tbody");
  
  thead.empty();
  thead.append('<tr><th>id</th><th>name</th><th>slug</th><th>filename</th><th>icon</th><th>url</th><th>icon</th></tr>');
  
  tbody.empty();
  
  var blocked = false;
  $.get(_warbands().first().icon.url).fail(blocked=true);
  _warbands().order("id").each(function (obj) {
    tbody.append (
      '<tr style="color: ' + obj.colour + ';">'
      + '<td>' + obj.id + '</td>'
      + '<td>' + obj.name + '</td>'
      + '<td>' + obj.slug + '</td>'
      + '<td>' + obj.icon.filename + '</td>'
      + '<td><img style="width: 25px" src="' + WHUWICONPATH + obj.icon.filename + '"></td>'
      + '<td>' + obj.icon.url + '</td>'
      + (blocked ? '<td title="blocked">X</td>' : '<td><img style="width: 25px" src="' + obj.icon.url + '"></td>')
      );
  });
}

function write_sets() {
  var blocked = false;
  $.get(_sets().first().icon.url).fail(blocked=true)
  var thead = $('#sets').find("thead");
  var tbody = $('#sets').find("tbody");
  
  thead.empty();
  thead.append('<tr><th>id</th><th>name</th><th>slug</th><th>filename</th><th>icon</th><th>url</th><th>icon</th></tr>');
  
  tbody.empty();
  _sets().order("id").each(function (obj) {
    tbody.append (
      '<tr>'
      + '<td>' + obj.id + '</td>'
      + '<td>' + obj.name + '</td>'
      + '<td>' + obj.slug + '</td>'
      + '<td>' + obj.icon.filename + '</td>'
      + '<td><img style="width: 25px" src="' + WHUWICONPATH + obj.icon.filename + '"></td>'
      + '<td>' + obj.icon.url + '</td>'
      + (blocked ? '<td title="blocked">X</td>' : '<td><img style="width: 25px" src="' + obj.icon.url + '"></td>')
      );
  });
}

function write_cardtypes() {
  var blocked = false;
  $.get(_cardtypes().first().icon.url).fail(blocked=true)
  
  var thead = $('#cardtypes').find("thead");
  var tbody = $('#cardtypes').find("tbody");
  
  thead.empty();
  thead.append('<tr><th>id</th><th>name</th><th>slug</th><th>type</th><th>filename</th><th>icon</th><th>url</th><th>icon</th></tr>');
  
  tbody.empty();
  _cardtypes().order("id").each(function (obj) {
    tbody.append (
      '<tr>'
      + '<td>' + obj.id + '</td>'
      + '<td>' + obj.name + '</td>'
      + '<td>' + obj.slug + '</td>'
      + '<td>' + obj.type + '</td>'
      + '<td>' + obj.icon.filename + '</td>'
      + '<td><img style="width: 25px" src="' + WHUWICONPATH + obj.icon.filename + '"></td>'
      + '<td>' + obj.icon.url + '</td>'
      + (blocked ? '<td title="blocked">X</td>' : '<td><img src="' + obj.icon.url + '"></td>')
      );
  });
}

function write_cards() {
  var tbody = $('#cards').find("tbody");
  tbody.empty();
  
  var blocked = false;  
  let firstresult = _cards(_filter).first();
  if (firstresult) {
    $.get(firstresult.url).fail(blocked=true);
    _cards(_filter).order("code").each(function (c) {
      tbody.append(cardrow(c,blocked))
    });
    //updateCardImages(tbody, blocked)
  }
}

function cardrow (c, b) {
  var rtn = "";
  
  rtn = '<tr>'
    + '<td>' + c.code + '</td>'
    + '<td>' + c.gw_id + '</td>'
    + '<td>' + c.name + '</td>'
    + '<td><img class="icon-sm" src="' + WHUWICONPATH + c.set_icon + '"></td>'
    + '<td><img class="icon-sm" src="' + WHUWICONPATH + c.warband_icon + '"></td>'
    + '<td><span title="' + c.rule + '">Hover</span></td>'
    + '<td>' + (typeof c.glory == 'undefined' ? '' : c.glory) + '</td>'
    + '<td>' + c.filename + '</td>'
    + '<td>' + c.url + '</td>'
    + '<td><img class="icon-sm cardthumb" src="' + (b ? WHUWCARDPATH + c.filename : c.url) + '"></td>'
    + '</tr>';
  return rtn;
}


$('#selectset').on('change', function () { update_filter("set_id",$(this).val()); });
$('#selectwarband').on('change', function () { update_filter("warband_id",$(this).val()); });

function update_filter ( fld, vals ) {
  delete _filter[fld];
  if (vals.length > 0) {
    _filter[fld] = vals.map(v=>parseInt(v));
  }
  write_cards();
}
