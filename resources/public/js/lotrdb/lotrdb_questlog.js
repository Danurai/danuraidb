_decklists = [];
_cards = [];
_quests = [];

$.getJSON('/lotrdb/api/data/decks', function (data) {
  _decklists = data;
  $.each(_decklists, function(id, deck) {
    $('#decklists').append('<option value="' + deck.name + '" />');
  });
});

$.getJSON('api/data/cards', function (data) {
  _cards = TAFFY(data.map(c=>$.extend(c,{"typeaheadname":c.normalname + ' (' + c.pack_code + ')'})));
});

$.getJSON('api/data/scenarios',function (d) {
  _quests = d;
});

$('#players').on('change', function () {
  var p = $(this).val();
  for (i=1; i<5; i++) {
    $('#p'+i+'stats').attr('hidden',i>p);
  }
  recalc();
});

$('#scenario').on('change',function () {
  $('#questid').val(_quests.filter(q=>q.name==$(this).val())[0].id);
});

$('[id$="deckname"]').on('change',function () {
  var deckname = $(this).val();
  var decklist = _decklists.filter(d=>d.name==deckname)[0];
  var spherebox = $('#p'+this.id[1]+'spheres');
    
  spherebox.empty();
  $('#p'+this.id[1]+'decklist').val('');
  
  if (typeof decklist !== 'undefined') {
    $('#p'+this.id[1]+'decklist').val(decklist.data);
    var codes = Object.keys(JSON.parse(decklist.data));
    var spheres = _cards({"code":codes}).distinct("sphere_code");
    
    $.each(spheres.filter(s=>s!="neutral"),function (id, sp) {
      spherebox.append('<span class="fa-lg mr-1 lotr-type-'+sp+'" />');
    });
  }
});

$('[id$="deadh"],[id$="dmgh"],[id$="threat"],#vp,#turns').on('change',function () { recalc(); });

function recalc() {
  // Subtotal all player Scores, then total
  var sub = 0;
  var tot = 0;
  for (i=1; i<=$('#players').val(); i++) {
    sub = parseInt($('#p'+i+'deadh').val())
        + parseInt($('#p'+i+'dmgh').val())
        + parseInt($('#p'+i+'threat').val());
    $('#p'+i+'score').val(sub);
    $('#p'+i+'scoreshown').html(sub);
    tot += sub;
  }
  
  tot += parseInt($('#turns').val()) * 10;
  tot -= parseInt($('#vp').val());
  $('#score').val(tot);
  $('#scoreshown').html(tot);
  
}

// Mini Deckbuilder

$('#modaldecklist').on('show.bs.modal',function (e) {
  var fg = $(e.relatedTarget).closest('.form-group');
  var dn = $(fg).find('input[name$="deckname"]').val();
  var dl = $(fg).find('input[name$="decklist"]').val();
  var pn = $(fg).find('input[name$="deckname"]').attr('name').substr(1,1);
  
  $('#mdeckname').val(dn);
  $('#mparsedecklist').val(dl);
  $('#mdecklistpretty').html(pretty_decklist(dl!=''?JSON.parse(dl):[],true));
  $('#mpnum').val(pn);
});

$('#mdecksave').on('click',function () {
  var pn = "p" + $('#mpnum').val();
  $('#'+pn+'deckname').val($('#mdeckname').val());
  $('#'+pn+'decklist').val($('#mparsedecklist').val());
  $('#modaldecklist').modal('hide');
  
});

// TYPEAHEAD

$('#mcardname').typeahead({
  hint: true, highlight: true, minLength: 2
},{
  name: 'lotrcardnames',
  source: 
    function findMatches (q, cb) {
      cb(_cards().order('typeaheadname').filter({"normalname":{"likenocase":q}}).select("typeaheadname"));}
})

$('#mcardqty')
  .on('click','.mqtybtn',function() {
    var dl = ($('#mparsedecklist').val()==""?{}:JSON.parse($('#mparsedecklist').val()))
    var re = /(.+?)\s\((\w+)\)/  ///([0-9])x\s(.+?)\s\((\w+)\)|(.+?)\s\((\w+)\)/
    var c = {};
    re.exec($('#mcardname').val());
    c.nname = RegExp.$1;
    c.pack_code = RegExp.$2;
    c.qty = $(this).val();
    
    var card = _cards({"normalname":{"isnocase":c.nname},"pack_code":{"isnocase":c.pack_code}}).first()
    
    dl[card.code] = c.qty;
    $('#mparsedecklist').val(JSON.stringify(dl));
    $('#mdecklistpretty').html(pretty_decklist(dl,true));
    $('#mcardname').val('');
  });


//$('#mdecklist').on('input',function() {
//  $(this).val()
//  var decklist = {};
//  var c = {};
//  var card;
//  $.each($(this).val().split('\n'), function (i,l) {
//    re.exec(l);
//    if (RegExp.$1=="") {
//      c.qty = 1;
//      c.nname = RegExp.$4;
//      c.pack_code = RegExp.$5;
//    } else {
//      c.qty = RegExp.$1
//      c.nname = RegExp.$2;
//      c.pack_code = RegExp.$3;
//    }
//    if (typeof card !== 'undefined') {decklist[card.code]=c.qty;}
//  });
//  $('#mparsedecklist').val(JSON.stringify(decklist));
//  $('#mdecklistpretty').html(pretty_decklist(decklist));
//});

$('#mdecklistpretty').on('click','.removecard',function() {
  var dl = ($('#mparsedecklist').val() != '' ? JSON.parse($('#mparsedecklist').val()) : '')
  delete dl[$(this).data('code')];
  $('#mparsedecklist').val(JSON.stringify(dl));
  $('#mdecklistpretty').html(pretty_decklist(dl,true));
});
  
function pretty_decklist (decklist, remove=false) {
  var outp = '';
  //if (decklist.len > 0) {
    var deck = _cards({"code":Object.keys(decklist)}).order("normalname").map(c=>$.extend(c,{"qty":decklist[c.code]}));
    outp += '<div class="decklist">';
    
    $.each(["hero","ally","attachment","event"], function (id, t) { //deck.map(c=>c.type_code).filter((v, i, s)=>s.indexOf(v)===i).sort()
      if (deck.filter(c=>c.type_code==t).length>0) {
        outp += '<div class="decklist-section mb-2">'
        outp += '<div class="mb-1" style="text-transform: capitalize"><b>' + t + ' (' + deck.filter(c=>c.type_code==t).length + ')</b></div>';
        $.each(deck.filter(c=>c.type_code==t), function (id, c) {
          outp += '<div>' 
            + (remove ? '<button class="btn btn-outline-danger btn-sm removecard mr-2" style="line-height: 0.5 !important" data-code="' + c.code + '" ><i class="fas fa-times fa-xs" /></button>' : '')
            + '<span>' + c.qty + 'x ' + c.name + ' (' + c.pack_code + ')</span>'
            + '</div>';
        });
        outp += '</div>';
      }
    });
  //}
  return outp;
}

// quest list decklist modal
$('#qlmodal').on('show.bs.modal',function (e) {
  var dl = $(e.relatedTarget).data('decklist')
  var dn = $(e.relatedTarget).html();
  
  $('#qlmodal').find('.modal-title').html(dn);
  $('#qlmodal').find('.modal-body').html(pretty_decklist(dl))
});