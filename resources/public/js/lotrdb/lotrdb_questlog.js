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
  var decklist = (_decklists.filter(d=>d.name==deckname)[0] || "[]");
  
  set_decklist( $(this).attr('id')[1], decklist.data);
});

function set_decklist ( pn, dl ) {
  var spherebox = $('#p'+pn+'spheres');
  var codes = Object.keys(JSON.parse(dl));
  $('#p'+pn+'decklist').val(dl);
  spherebox.empty();  
  $.each(_cards({"code":codes,"sphere_code":{"!=":"neutral"}}).distinct("sphere_code"),function (id, sp) {
    spherebox.append('<span class="fa-lg mr-1 lotr-type-'+sp+'" />');
  }); 
};

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
    c.qty = parseInt($(this).val());
    
    var card = _cards({"normalname":{"isnocase":c.nname},"pack_code":{"isnocase":c.pack_code}}).first()
    
    dl[card.code] = (card.type_code == "hero" ? 1 : c.qty);
    $('#mparsedecklist').val(JSON.stringify(dl));
    $('#mdecklistpretty').html(pretty_decklist(dl,true));
    $('#mcardname').val('');
  });

$('#mdecklistpretty').on('click','.removecard',function() {
  var dl = ($('#mparsedecklist').val() != '' ? JSON.parse($('#mparsedecklist').val()) : '')
  delete dl[$(this).data('code')];
  $('#mparsedecklist').val(JSON.stringify(dl));
  $('#mdecklistpretty').html(pretty_decklist(dl,true));
});


$('#mdecksave').on('click',function () {
  var pn = "p" + $('#mpnum').val();
  $('#'+pn+'deckname').val($('#mdeckname').val());
  set_decklist($('#mpnum').val(), $('#mparsedecklist').val())
  $('#modaldecklist').modal('hide');
});

  
function pretty_decklist (decklist, remove=false) {
  var outp = '';
  var deck = _cards({"code":Object.keys(decklist)}).order("normalname").map(c=>$.extend(c,{"qty":decklist[c.code]}));
  outp += '<div class="decklist">';
  
  $.each(["hero","ally","attachment","event"], function (id, t) { //deck.map(c=>c.type_code).filter((v, i, s)=>s.indexOf(v)===i).sort()
    if (deck.filter(c=>c.type_code==t).length>0) {
      outp += '<div class="decklist-section mb-2">'
      outp += '<div class="mb-1" style="text-transform: capitalize"><b>' + t + ' (' + deck.filter(c=>c.type_code==t).reduce((s,c)=>s+=c.qty,0) + ')</b></div>';
      $.each(deck.filter(c=>c.type_code==t), function (id, c) {
        outp += '<div>' 
          + (remove ? '<button class="btn btn-xs btn-outline-danger removecard mr-2" style="line-height: 0.5 !important" data-code="' + c.code + '" ><i class="fas fa-times fa-xs" /></button>' : '')
          + '<span>' + c.qty + 'x ' 
          + (c.is_unique ? '<span class="mr-1 lotr-type-unique" />' : '')
          + c.name + ' (' + c.pack_code + ')</span>'
          + '<span class="ml-2 lotr-type-' + c.sphere_code + '" />'
          + '</div>';
      });
      outp += '</div>';
    }
  });
  return outp;
}

$('#qlmodal')
  .on('show.bs.modal',function (e) {
    var rt = e.relatedTarget;
    var dh, db, df = '';
    if ($(rt).hasClass('btn-delete')) {
      var qid = $(rt).data('qid');
      dh = 'Delete Quest: #' + qid;
      db = 'Are you sure you want to delete quest id #' + qid + '?'
      df = '<button class="btn btn-secondary" data-dismiss="modal">Cancel</button>'
        + '<form action="/lotrdb/questlog/delete" method="post">'
        + '<input name="qid" readonly hidden value=' + qid + '>'
        + '<button class="btn btn-danger">OK</button>'
        + '</form>';
    } else {
  // quest list decklist modal
      dh = $(rt).html();
      db = pretty_decklist($(e.relatedTarget).data('decklist'));
      df = '<button class="btn btn-secondary" data-dismiss="modal">Close</button>';
    }
    $('#qlmodal').find('.modal-title').html(dh);
    $('#qlmodal').find('.modal-body').html(db);
    $('#qlmodal').find('.modal-footer').html(df);
  })
  .on('hide.bs.modal', function () {
    $('#qlmodal').find('.modal-title').html('');
    $('#qlmodal').find('.modal-body').html('');
    $('#qlmodal').find('.modal-footer').html('');
  });
  
// Edit & Reset

$('li.btn-edit').on('click', function () {
  resetquest();
});

$('#resetquest').on('click',function () {
  resetquest();
});

function resetquest() {
  var form = $(this).closest('form');
  $(form).find('input[type="text"]').val('');
  $(form).find('input[type="number"]').val(0);
}