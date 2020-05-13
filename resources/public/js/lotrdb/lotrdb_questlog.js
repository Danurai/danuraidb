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
  _cards = TAFFY(data);
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
  if (typeof decklist !== 'undefined') {
    $('#p'+this.id[1]+'decklist').val(decklist.data);
    var codes = Object.keys(JSON.parse(decklist.data));
    var spheres = _cards({"code":codes}).distinct("sphere_code");
    var spherebox = $('#p'+this.id[1]+'spheres');
    spherebox.empty();
    
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
    $('#p'+i+'subtotal').html(sub);
    tot += sub;
  }
  
  tot += parseInt($('#turns').val()) * 10;
  tot -= parseInt($('#vp').val());
  $('#score').html(tot);
  
}

// SAVE

$('form').on('submit',function (e) { 
  e.preventDefault() }
  );
$('#savequest').on('click',function () {
  // Build data
  var savedata = {};
  var pstats = {};
  savedata.questid = parseInt($('#questid').val());
  savedata.difficulty = $('#difficulty').val();
  savedata.players = parseInt($('#players').val());
  savedata.vp = parseInt($('#vp').val());
  savedata.turns = parseInt($('#turns').val());
  savedata.date = new Date($('#date').val()).getTime();
  savedata.score = parseInt($('#score').html());
  savedata.plyrstats = [];
  
  for (i=1; i<=parseInt($('#players').val()); i++) {
    pstats = {};
    pstats.deckname = $('#p'+i+'deckname').val();
    pstats.decklist = $('#p'+i+'decklist').val();
    pstats.deadh = parseInt($('#p'+i+'deadh').val());
    pstats.dmgh = parseInt($('#p'+i+'dmgh').val());
    pstats.threat = parseInt($('#p'+i+'threat').val());
    pstats.score = parseInt($('#p'+i+'subtotal').html());
    savedata.plyrstats.push(pstats);
  }
  
  console.log(savedata);
});