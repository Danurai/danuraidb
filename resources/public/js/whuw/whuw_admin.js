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

function write_warbands() {
  var thead = $('#warbands').find("thead");
  var tbody = $('#warbands').find("tbody");
  
  thead.empty();
  thead.append('<tr><th>id</th><th>name</th><th>slug</th><th>filename</th><th>icon</th><th>url</th><th>icon</th></tr>');
  
  tbody.empty();
  _warbands().order("id").each(function (obj) {
    tbody.append (
      '<tr style="color: ' + obj.colour + ';">'
      + '<td>' + obj.id + '</td>'
      + '<td>' + obj.name + '</td>'
      + '<td>' + obj.slug + '</td>'
      + '<td>' + obj.icon.filename + '</td>'
      + '<td><img style="width: 25px" src="' + WHUWICONPATH + obj.icon.filename + '"></td>'
      + '<td>' + obj.icon.url + '</td>'
      + '<td><img style="width: 25px" src="' + obj.icon.url + '"></td>'
      );
  });
}

function write_sets() {
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
      + '<td><img style="width: 25px" src="' + obj.icon.url + '"></td>'
      );
  });
}

function write_cardtypes() {
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
      + '<td><img src="' + obj.icon.url + '"></td>'
      );
  });
}

function write_cards() {
  var tbody = $('#cards').find("tbody");
  tbody.empty();
  
  _cards(_filter).order("code").each(function (c) {
    tbody.append(cardrow(c))
  });
  updateCardImages(tbody)
}

function cardrow (c) {
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
    + '<td><img class="icon-sm cardthumb" data-filename="' + c.filename + '" data-url="' + c.url + '"></td>'
    + '</tr>';
  return rtn;
}

function updateCardImages (tbody) {
  tbody.find('.cardthumb').each(function (id, ele) {
    $.get(WHUWCARDPATH + $(ele).data("filename"), function (img) {
      ele.src = WHUWCARDPATH + $(ele).data("filename");
    }).fail( function () {
      ele.src = $(ele).data("url");           
    });
  });
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