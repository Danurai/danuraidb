var _warbands;
var _cardtypes;
var _sets;
var _cards;
var filter = {};

$(document).ready(function () {
  $.getJSON("/whuw/api/data", function (data) {
     _warbands = TAFFY(data.warbands);
     _cardtypes = TAFFY(data["card-types"]);
     _sets = TAFFY(data["sets"]);
     
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
        + '<td><img style="width: 25px" src="/img/icons/' + obj.icon.filename + '"></td>'
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
        + '<td><img style="width: 25px" src="/img/icons/' + obj.icon.filename + '"></td>'
        + '<td>' + obj.icon.url + '</td>'
        //+ '<td><img src="' + obj.icon.url + '"></td>'
        );
    });
  }
  
  function write_cardtypes() {
    var thead = $('#cardtypes').find("thead");
    var tbody = $('#cardtypes').find("tbody");
    
    thead.empty();
    thead.append('<tr><th>id</th><th>name</th><th>slug</th><th>type</th><th>filename</th><th>icon</th><th>url</th><th>icon</th></tr>');
    
    tbody.empty();
    _cardtypes(filter).order("id").each(function (obj) {
      tbody.append (
        '<tr>'
        + '<td>' + obj.id + '</td>'
        + '<td>' + obj.name + '</td>'
        + '<td>' + obj.slug + '</td>'
        + '<td>' + obj.type + '</td>'
        + '<td>' + obj.icon.filename + '</td>'
        + '<td><img style="width: 25px" src="/img/icons/' + obj.icon.filename + '"></td>'
        + '<td>' + obj.icon.url + '</td>'
        //+ '<td><img src="' + obj.icon.url + '"></td>'
        );
    });
  }
  
  function write_cards() {
    var tbody = $('#cards').find("tbody");
    tbody.empty();
    
    _cards(filter).order("code").each(function (c) {
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
      + '<td><img class="icon-sm" src="/img/icons/' + c.set_icon + '"></td>'
      + '<td><img class="icon-sm" src="/img/icons/' + c.warband_icon + '"></td>'
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
      $.get("/img/cards/" + $(ele).data("filename"), function (img) {
        ele.src = "/img/cards/" + $(ele).data("filename");
      }).fail( function () {
        ele.src = $(ele).data("url");
      });
    })
  }
  
  
   $('#selectset').on('change', function() {
    vals = $(this).val();
    delete filter.set;
    if (vals.length > 0) {
      filter.set = vals.map(x => parseInt(x));
    }
    write_cards();
  });
  
  $('#selectwarband').on('change', function() {
    vals = $(this).val();
    delete filter.warband;
    if (vals.length > 0) {
      filter.warband = vals.map(x => parseInt(x));
    }
    write_cards();
  });
  
});