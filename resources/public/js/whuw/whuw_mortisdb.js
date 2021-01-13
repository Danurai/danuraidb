let _champ;
let _deck = new Set();
let _cards;
let _champions;

$.fn.selectpicker.Constructor.DEFAULTS.multipleSeparator = " ";
$.fn.selectpicker.Constructor.DEFAULTS.noneSelectedText = "(All)";

let DT = $('#cardtbl').DataTable({
  lengthChange: false,
  pageLength: 100,
  //paging: false,
  dom: 'st<"small"p><"small"i>', 
  columnDefs: [
    {orderable: false, targets: [0]}],
  order: [[1, "asc"]]
});

$.getJSON('/whuw/api/champions', function (data) {
  _champions = TAFFY(data.champions);
  
  let champid = $('#deck-alliance').val();
  if (champid != "") {
    _champ = data.champions.filter(ch=>ch.id==parseInt(champid,10))[0]
    if ($('#deck-name').val() == "") {$('#deck-name').val(_champ.name + ' deck')}
    $('#selectchamp').selectpicker('val',champid);
  } else {
    _champ = _champions().first();
    $('#deck-name').val(_champ.name + ' deck');
    $('#deck-alliance').val(_champ.id);
  }
  if ($('#deck-content').val() != "") {
    _deck = new Set(JSON.parse($('#deck-content').val()))
  }
  
  $.getJSON('/whuw/api/cards', function (data) {
    _data = data.map(function (c) {if (typeof c.glory == "undefined") {c.glory = 0} return c});
    _cards = TAFFY(data);
    setWarbandFilter(_champ);
    let sets = localStorage.getItem("whuw_set_collection");
    if (sets) {
      $('#selectset').selectpicker('val',JSON.parse(sets));
      DT.column(3).search(JSON.parse(sets).map(s=>`^${s}$`).join("|"),true,false);
    }
    DT.column(7).search("true").draw();
    writeDecklist(_champ,_deck);
    
    //update table
    Array.from(_deck).forEach(function (code) {
      $('#cardtbl').find(`svg[data-code="${code}"]`).removeClass("fa-square");
      $('#cardtbl').find(`svg[data-code="${code}"]`).addClass("fa-check-square");
    });
  
  });
});

$('#cardtbl').on('click','.tblcheck',function() {
  let code = $(this).data('code');
  if ($(this).hasClass("fa-square")) {
    _deck.add(code);
  } else {
    _deck.delete(code);
  }
  $(this).toggleClass("fa-square")
  $(this).toggleClass("fa-check-square");
  
  writeDecklist(_champ,_deck);
});


function deckcheck (champ, deck) {
  //let wnd_to_upg [3,3,3,2,2,1,0,0,0,0,0,0]
  //let upgrades = '<span class="mr-1">' + wnd_to_upg[champ.wounds] + '</span>';
  return '<div>Minimum 10 Ploys and 10 Upgrades</div>'
}

function writeDecklist(champ, deck) {
  let fname = champ.cards[1].url.match(/\/([\w|\-|\_]+\.png)$/)[1];
  
  //update table
  Array.from(deck).forEach(function (code) {
    $('#cardtbl').find(`svg[data-code="${code}"]`).removeClass("fa-square");
    $('#cardtbl').find(`svg[data-code="${code}"]`).addClass("fa-check-square");
  });
  
  $('#deck-content').val(JSON.stringify(Array.from(_deck)));
  $('#champimg').attr('src',`/img/whuw/champs/${fname}`);
  $('#decklist').empty();
  
  $('#decklist').append(deckcheck(champ,deck));
  
  [["Ploys",[21,150]],["Upgrades",22]].forEach(function ([typename,typeid]) {
    let count = _cards({"code":Array.from(deck),"card_type_id":typeid}).count();
    let seg = $('<div></div>');
    $('#decklist').append(`<div class="decklist-section"><b>${typename}</b>&nbsp;(${count})</div>`);
    $('#decklist').append(seg);
    _cards({"code":Array.from(deck),"card_type_id":typeid}).order("name").each(function (crd) {
      seg.append(`<span class="deckrmv" data-code="${crd.code}"><i class="fa fa-times-circle text-secondary fa-sm mr-1"></i><a href="#" class="cardlink" data-code="${crd.code}">${crd.name}`
        + (crd.target != "-" ? ` (${crd.target})` : '')
        + '</a><br />');
    });
    //seg.append(_cards({"code":Array.from(deck),"card_type_id":typeid}).order("name").supplant('<span class="deckrmv" data-code="{code}"><i class="fa fa-times-circle text-secondary fa-sm mr-1"></i><a href="#" class="cardlink" data-code="{code}">{name}</a><br />'));
  });
  
}

$('#champimg').on('click', function () {
  writeDecklist(_champ, _deck);
});

$('#champmodal').on('show.bs.modal',function (evt) {
  $('#champmodal img').attr('src',$(evt.relatedTarget).attr('src'));
});

$('#decklist').on('click','.deckrmv',function() {
  _deck.delete($(this).data('code'));
  writeDecklist(_champ,_deck);
});
// Filters

function setWarbandFilter(champ) {
  DT.column(4).search(`^${champ.warband_id}$|^35$`,true,false).draw();
  $('#warbandfilter').empty();
  let togglegroup = $('<div class="btn-group-toggle" data-toggle="buttons"></div>');
  $('#warbandfilter').append(togglegroup);
  _cards({"warband_id":[champ.warband_id,35]}).distinct("warband_id","warband_icon","warband_name").forEach(function ([id,icon,name]) {
    togglegroup.append(`<label class="btn btn-sm btn-light"><input type="checkbox" data-warband_id="${id}"></input><img class="icon-sm" src="/img/whuw/icons/${icon}" title="${name}"></img></label>`);
  });
}

$('#warbandfilter').on('change', function () {
  let show_wb = Array.from($('#warbandfilter').find('input:not(:checked)')).map(c=>'^'+$(c).data('warband_id')+'$').join('|');
  DT.column(4).search(show_wb,true,false).draw();
});

$('#championstoggle').on('click', function () {
  if ($(this).hasClass('active')) {
    DT.column(7).search("").draw();
  } else {
    DT.column(7).search(true).draw();
  }
});

$('#selectchamp').on('change', function () {
  _champ = _champions({"id":parseInt($(this).selectpicker('val'),10)}).first();
  $('#deck-name').val(_champ.name + ' deck');
  $('#deck-alliance').val(_champ.id);
  setWarbandFilter(_champ);
  writeDecklist(_champ,_deck);
})

$('#selectset').on('change',function() {
  let sets = $('#selectset').selectpicker().val();
  let flt = sets.map(c=>"^"+c+"$").join("|");
  DT.column(3).search(flt,true,false).draw();
  localStorage.setItem("whuw_set_collection",JSON.stringify($('#selectset').selectpicker('val')));
});

$('#selecttype').on('change',function() {
  let flt = $('#selecttype').selectpicker().val().map(c=>"^"+c+"$").join("|");
  DT.column(2).search(flt,true,false).draw();
});

$('#search')
  .on('input', function () {
    DT.column(1).search($(this).val()).draw();
  })
  .typeahead({
    highlight: true,
    minLength: 3
  },{
    source: function (qry,cb) {
      cb(DT.column(1, {filter: 'applied'}).nodes()
        .map(c=>$(c).text()))
  }})
  .on('typeahead:select typeahead:autocomplete', function(ev, suggestion) {
    DT.column(1).search(suggestion).draw();
  }); 