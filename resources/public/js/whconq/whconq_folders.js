var pagenum = 0;
var section = [];
var factions = [];
var pages = [];

var _cycles;
var _packs;
var _cards;

var imageUrlTemplate;

var outp = '';
var checked = false;
var _factions;
var _types;

var filter_pack = [];
var faction_code;

var storekey = "whk-fld-pack-";


// LOAD DATA
$.getJSON('/whconq/api/cycles', function (data) {      //,{_: new Date().getTime()}
  _cycles = TAFFY(data);
  $.getJSON('/whconq/api/packs', function (data) {
    _packs = TAFFY(data);	
    $.getJSON('/whconq/api/factions', function (data) {
      _factions = TAFFY(data);
      $.getJSON('/whconq/api/types', function (data) {
        _types = TAFFY(data);
        $.getJSON('/whconq/api/cards', function (data) {
          _cards = TAFFY(data);
          
          // Build PACKS Checklist
          _cycles().order("position").each( function (cycle) {
            indent = false;
            cyclepacks = _packs({"cycle_code":cycle.code}).select("code");
            _packs({"cycle_code":cycle.code}).order("position").each( function (pack,idx) {
                            
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
            });
            if (indent == true)	{
              outp += '</ul>';
            }
          });
          $('#packlist').html (outp);
          
          // Set PACKS Checkboxes
          if (typeof(Storage) !== "undefined")	{
            _packs().order("position").each( function (pack,idx) {
              // get localstorage value
              checked = localStorage.getItem(storekey + pack.code) == null ? true : localStorage.getItem(storekey + pack.code) != "false";
              // un-check child and parent
              if (checked == false)	{
                $('#packlist').find('input[data-code="' + pack.code + '"]').prop("checked",checked);
                $('#packlist').find('input.cycle[data-cycle="' + pack.cycle_code + '"]').prop("checked",checked);
              }
            });
          }
          
          // Initialise Pack filter for list of cards
          faction_code = 'space_marines';
          set_filter_pack();
          // PACKS :: END
                  
          // FACTIONS Breadcrumb Nav
          outp = '<ol class="breadcrumb bg-light">';
          _factions().each( function (faction, idx)	{	//.order("side_code,name")
            outp += '<li data-faction="' + faction.code + '" class="breadcrumb-item">'
                  + '<a href="#" style="color: ' + faction.color + '">' + faction.name + '</a></li>';
          });
          outp += '<li data-faction="planets" class="breadcrumb-item"><a href="#">Planets</a></li>';
          outp += '</ol>';
          
          $('#foldersections').html(outp);
          
          updateFolder()
          
        });
      });
    });
  });
});

// Data Pack Filter: filter_pack
function set_filter_pack()	{
  filter_pack = [];
  _packs().order("position").each( function (pack) {
    if (typeof(Storage) !== "undefined") {
      if (localStorage.getItem(storekey + pack.code) == null || localStorage.getItem(storekey + pack.code) == "true") {
        filter_pack.push(pack.code);
      }
    } else {
      filter_pack.push(pack.code)
    }
  });
  updateFolder();
}

function updateFolder()	{
  pages = [];
  
  if (faction_code == "planets") {
    _cards({"type_code":"planet"}).each(function (card) {
      pages.push (card);
    });
  } else {
    _types({"code":{"!is":"token"}}).order("position").each(function (type)	{
      if (type.name == "Warlord Unit"){
        _cards({"faction_code":faction_code,"type":type.name,"pack_code":filter_pack}).each(function (warlord) {
          pages.push (warlord);
          //pages.push ($.extend(warlord, {"text":"Bloodied"}));
          pages.push({"name":""});
          pages.push({"name":""});
          pages = pages.concat(_cards({"signature_squad":warlord.signature_squad,"type_code":{"!is":"warlord_unit"}}).order("position").get());
          
          while (pages.length % 9 != 0) { pages.push({"name":""}); }
        });
      } else {
        _cards({"faction_code":faction_code,
                 "type":type.name,
                 "pack_code":filter_pack,
                 "signature_loyal":{"!is":"Signature"}}).order("name").each (function (card) {
          pages.push (card);
        });
      }
      // Fill page with empty slots
      if (_cards({"faction_code":faction_code,"pack_code":filter_pack}).count() > 9) {
        while (pages.length % 9 != 0) { pages.push({"name":""}); }
      }
    });
  }
  while (pages.length % 9 != 0) { pages.push({"name":""}); }
  drawpage();
  updateCardCounts();
}

function drawpage()	{
  var pagecount = (pages.length/9);
  var outp = '';
  var pager = '';
  
  pagenum = Math.max(0,pagenum);
  pagenum = Math.min(pagecount-1,pagenum);
  
// Pagination
  pager = '<ul class="pagination">';
  pager += '<li class="page-item">'
        + '<a class="page-link' + (pagenum == 0 ? ' disabled' : '')
        + '" data-page="previous">'
        + '<i class="fas fa-angle-double-left"></i></a></li>';
        
  for (var i=0; i<pagecount; i++) {
    pager += '<li class="page-item' + (i == pagenum ? ' active' : '') + '">'
            + '<a class="page-link" data-page="' + i + '">' + (i + 1) + '</a>'
            + '</li>';
  }
  pager += '<li class="page-item">'
        + '<a class="page-link' + (pagenum == pagecount ? ' disabled' : '')
        + '" data-page="next">'
        + '<i class="fas fa-angle-double-right"></i></a></li>';
  
  $('#folderpager').html( pager );
  
  outp = cardCards();
  $('#folderpages').html(outp);
}

function cardCards() {
  var outp = '';
  var id;  
  
  if (pages.length == 0) {
    outp = 'You do not own any ' + _factions({"code":faction_code}).first().name + ' cards.';
  } else {
    outp += '<div class="container">'; // reset card widths
    outp += '<span>'+ _factions({"code":faction_code}).first().name  + ' :: ' + _types({"code":pages[pagenum*9].type_code}).first().name + '</span>';
    
    for (var i=0; i<3; i++) {
      outp += '<div class="card-deck my-1">';
      for (var x=0; x<3; x++) {
        id = (pagenum*9)+(i*3)+x;
        outp += '<div class="card">';
        if (pages[id].name != "") {
          if ($('#showimg').prop('checked'))	{
            outp += '<img class="card-img" src="' + pages[id].img + '" alt="' + pages[id].name + '">';
          } else {
            outp += '<div class="card-body card-tooltip" data-code="' + pages[id].code.toString() + '">'
                  + '<h4 class="card-title">' + pages[id].name + '</h4>'
                  + '</div>';
          }
        // footer
          outp += '<div class="card-footer d-flex justify-content-between">'
            + '<small>' + pages[id].pack + '&nbsp;#' + parseInt(pages[id].position,10) + '</small>'
            + (pages[id].signature_loyal == 'Signature' ? '<span><i class="fas fa-cog icon-sig"></i> x' + pages[id].quantity + '</span>' : '' )
            + (pages[id].signature_loyal == 'Loyal' ? '<i class="fas fa-crosshairs icon-loyal"></i>' : '' )
            + '</div>';
        }
        outp += '</div>';
      }
      outp += '</div>';
    }
    outp += '</div>';
  }
  return outp;
}

function updateCardCounts() {
  let total = 0;
  
  $('#cardcounts').empty();
  
  filter_pack.forEach( fp => {
    let pack_count = _cards({"pack_code":fp}).sum("quantity");
    if (fp == 'core') {
      pack_count *= $('#coresetcount').find('input:checked').val();
    }
    total += pack_count;
    $('#cardcounts').append(`<div class="d-flex"><div>${_packs({"code":fp}).first().name}</div><div class="ml-auto">${pack_count}</div></div>`) 
  });
  $('#cardcounts').append(`<div class="d-flex"><b>Total</b><b class="ml-auto">${total}</b></div>`)
}

// Listeners //
$('#coresetcount').on('input', e => updateCardCounts() );

$('#foldersections').on('click','li',function () {
  pagenum = 0;
  section = factions[$(this).data('faction')];
  faction_code = $(this).data('faction');
  updateFolder();
});

$('#packlist')
  .on('click','input.pack',function () {
    localStorage.setItem(storekey + $(this).data('code'),$(this).prop('checked').toString() );
    // if ($(this).prop('checked')) {localStorage.setItem(storekey + $(this).data('code'),"true"} else {localStorage.removeItem(storekey + $(this).data('code'))}
    set_filter_pack();
  })
// Parent/child checkboxes
  .on("click","div.form-check.pa",function()	{
    var packcode;
    var checkchild = $(this).find("input[type='checkbox']").prop("checked");
    var cyclecode = $(this).find("input[type='checkbox']").data("cycle");
    $(this).parent().find("[data-cycle='" + cyclecode + "']input[type='checkbox'].pack").each(function (id,chkbox)	{
      $(chkbox).prop("checked",checkchild);
      packcode = $(chkbox).data('code');
      localStorage.setItem(storekey + packcode,checkchild);
    });
    set_filter_pack();
  })
  .on("click","div.form-check.ch",function()	{
    var checkpar = $(this).parent().find("input[type='checkbox']:checked").length == $(this).parent().find("input[type='checkbox']").length;
    var cyclecode = $(this).find("input[type='checkbox']").data("cycle");
    $(this).parent().parent().find("div.form-check.pa [data-cycle='" + cyclecode + "']input[type='checkbox']").prop("checked",checkpar);
  });

// Folder Page Nav
$('#folderpager').on('click','a',function(e)  {
  e.preventDefault();
  switch ($(this).data('page')) {
    case 'previous':
      pagenum--;
      break;
    case 'next':
      pagenum++;
      break;
    default:
      pagenum = $(this).data('page');
  }
  drawpage();
});


// Show Card Images checkbox
$('#showimg').on('change',function() {
  drawpage();
});