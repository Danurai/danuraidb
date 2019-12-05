// SETUP

var _db_cards;
var _db_packs;
var DT 

$.get("/lotrdb/api/data/cards", function(data) {
  _db_cards = data.filter(c=>c.pack_code!="Starter"); 
  $.getJSON('/lotrdb/api/data/packs',function (data) {
    _db_packs = TAFFY(data);
    initDataTable();
    write_decklist(0);
    write_decklist(1);
    initpacks();
  });
});


/************************************************************/
function initDataTable () {
  DT = $('#dt').DataTable({
    pageLength: 25,
    lengthChange: false,
    dom: 'tp<"small"i>',
    columnDefs: [
      {orderable: false, targets: [0,9]},
      {type: "num", targets: [4,5,6,7,8,10]} ]
  });
}

function initpacks () {  
// Packs
  var packsowned = JSON.parse(localStorage.getItem('lotrpacks_owned'));
  if (packsowned != null) {
    $.each(packsowned,function (id,c) {
      var ele = $('#packlist').find('input[data-code="' + c + '"]')
      ele.attr('checked',true);
      setparentcheckbox(ele);
    });
    DT.column(10).search(packsowned.join('|'),true).draw();
  }
  var coreowned = JSON.parse(localStorage.getItem('lotrcore_owned')) || 1;
  $('#coresets').find('input[value="' + coreowned + '"]').closest('label').button('toggle');
}

function setparentcheckbox(ele) {
  var $li = ele.closest('li');
  var check_count = $li.find('input:checked[data-type="pack"]').length;
  $li.find('input').prop('indeterminate',false);
  if (check_count == ($li.find('input').length - 1)) {
    $li.find('input').attr('checked',true);
  } else if (check_count > 0) {
    $li.find('input[data-type="cycle"]').prop('indeterminate',true);
  } else {
    $li.find('input').attr('checked',false);
  }  
}

function set_table_buttons () {
  for (i=0; i<2; i++) {
    DT.cells('[data-deck=' + i +  ']').nodes().to$().find('button').removeClass('active');
    DT.cells('[data-deck=' + i +  ']').nodes().to$().find('button[value=0]').addClass('active');
    
    if ($('#deckdata' + i).val() != "") {
      $.each(JSON.parse($('#deckdata' + i).val()), function (k, v) {
        DT.cells('[data-code="' + k + '"][data-deck=' + i + ']').nodes().to$().find('button[value=0]').removeClass('active');
        DT.cells('[data-code="' + k + '"][data-deck=' + i + ']').nodes().to$().find('button[value=' + v + ']').addClass('active');
      });
    }
  }
}

// TOOLS
function write_decklist(id) {
  var decklist;
  if ($('#deckdata' + id).val() == "") {
    decklist = {};
  } else {
    decklist = JSON.parse($('#deckdata' + id).val());
  }
  var deckcards = TAFFY(_db_cards)({"code": Object.keys(decklist)}).order("name").map(c=>$.extend({"qty": decklist[c.code]},c))
  var outp = '';
  outp = '<div class="d-flex mb-2">';
  $.each(deckcards.filter(c=>c.type_code == "hero"),function (i, c) {
    outp += '<a class="card-link" data-target="#cardmodal" data-toggle="modal" '
      + 'href="lotrdb/card/' + c.code + '" '
      + 'data-code="' + c.code + '">'
      + '<div class="deckhero" '
      + 'style = "background-image: url(' + getcgdbImageUrl(c) + '); position: relative;">'
      + '<span style="position: absolute; right: 2px; bottom: 2px;">'
      + '<image src="/img/lotrdb/icons/sphere_' + c.sphere_code + '.png" style="width: 35px;" />'
      + '</span>'
      + '</div></a>';
  });
  outp+='<div class="p-3">' + deckcards.filter(c=>c.type_code != "hero").reduce((t,c)=>t+=parseInt(c.qty),0) + '/50 cards</div>';
  outp+='</div>';
  outp += '<div style="-webkit-column-gap: 20px; -webkit-column-count: 2; -moz-column-gap: 20px; -moz-column-count: 2; column-gap: 20px; column-count: 2;">';
  $.each(["Ally","Attachment","Event"],function (n, t) {
    var cardsOfType = deckcards.filter(c=>c.type_name == t);
    if (cardsOfType.length > 0) {
      outp += '<div style="break-inside: avoid;"><span class="font-weight-bold">' + t + ' (' + cardsOfType.reduce((t,c)=>t+=parseInt(c.qty),0) + ')</span>';
      $.each(deckcards.filter(c=>c.type_name == t), function (i, c) {
        outp += '<div>' + c.qty + 'x <a class="card-link" data-toggle="modal" data-target="#cardmodal" data-code="' + c.code + '" href="/lotrdb/card/' + c.code + '">' + c.name + '</a></div>';
      });
      outp+='</div>';
    }
  });
  outp+='</div>';
    
  $('#decklist' + id).html(outp);
}



//LISTENERS

// Load and Save

$('#fellowshipname').on('input',function () {
  $('#savefellowship').attr('disabled',false);
});

$('#saveform').on('submit', function (ev) {
  ev.preventDefault();
  ev.stopPropagation();
  
  var fellowship = {};
  this.classList.add('was-validated');
  
  if (this.checkValidity() === true) {
    // Save Decks
    fellowship.decks = JSON.stringify([$('#deckid0').val(),$('#deckid1').val()]);
    fellowship.name = $('#fellowshipname').val();
    fellowship.id = $('#fellowshipid').val()
    if (fellowship.id.search(/[0-9A-F]{6}/) == -1) {
       delete fellowship.id
    }
    
    $.post("/decks/fellowship/save", fellowship, function (r) {
      $('#fellowshipid').val(r);
      showToast(-1);
      //alert ("Toast" + fellowship);
      $('#savefellowship').attr('disabled',true);
    });
  }
});

$('form[id^="formdeck"]').on('submit',function(ev) {
  ev.preventDefault();
  ev.stopPropagation();
  
  var id = $(this).attr('id').substr(-1);
  var deck = {};
  this.classList.add('was-validated');
  
  if (this.checkValidity() == true) {
    deck.name = $('#deckname' + id).val();
    deck.data = $('#deckdata' + id).val();
    deck.id =   $('#deckid' + id).val();
    if (deck.id.search(/[0-9A-F]{6}/) == -1) {
      delete deck.id;
    }
    $.post("/decks/fellowship/savedeck",deck,function(r){
      $('#deckid' + id).val(r);
      showToast(id);
      $('#savedeck'+id).attr('disabled',true);
    });
  }
});

function showToast(id) {
  var title = (id == -1 ? "Fellowship Saved" : "Deck Saved")
  var body = (id == -1 
    ? '<span>Fellowship <b>' + $('#fellowshipname').val() + '</b> has been saved.</span>'
    : '<span>Deck <b>' + $('#deckname'+id).val() + '</b> has been saved.</span>')
  $('#toast').find('.toast-title').html(title);
  $('#toast').find('.toast-body').html(body);
  $('#toast').toast({delay:5000}).toast('show');
}

$('#loadmodal').on('show.bs.modal', function (ev) {
  var deckno = $(ev.relatedTarget).data('deckno');
  $.getJSON('/lotrdb/api/data/userdecks', function (data) {
    $('#loadmodal').find('ul').empty();
    $.each(data,function(id,deck) {
      $('#loadmodal').find('ul').append (
        '<li class="btn btn-light list-group-item" role="button" data-code="' + deck.uid + '" data-deckno=' + deckno + ' data-dismiss="modal"><div class="d-flex" >'
        + '<b>' + deck.name + '</b>'
        + '</div></li>')
    });
  });
});

$('#loadmodal').on('click','li',function() {
  var deckno = $(this).data('deckno');
  var deckcode = $(this).data('code');
  $.getJSON('/lotrdb/api/data/userdecks', function (data) {
    var deck = data.filter(c=>c.uid==deckcode)[0];
    $('#deckname' + deckno).val(deck.name);
    $('#deckdata' + deckno).val(deck.data);
    $('#deckid' + deckno).val(deck.uid);
    write_decklist(deckno);
    set_table_buttons();
  });
});

$('[id^="deckname"]').on('input',function() {
  $('#savedeck' +  this.id.substr(-1)).attr('disabled',false);
});

// Filter and Search 

$('#typefilter').on('change',function() {
  var filter = $(this).find('input:checked').toArray().map(c=>c.name).join("|");
  DT.column(3).search(filter,true).draw();
});

$('#spherefilter').on('change',function() {
  var filter = $(this).find('input:checked').toArray().map(c=>c.name).join("|");
  DT.column(2).search(filter,true).draw();
});

$('#search')
  .on('input', function () {
    var res = /t:(.+)/.exec($(this).val());
    if (res != null) {
      DT.column(1).search("").draw();
      DT.column(11).search(res[1]).draw();
    } else {
      DT.column(11).search("").draw();
      DT.column(1).search($(this).val()).draw();
    }
  })
  .typeahead({
    highlight: true,
    minLength: 3
  },{
    source: function (qry,cb) {
      cb(DT.column(1, {filter: 'applied'}).nodes()
        .map(c=>$(c).data('filter'))
        .filter(c=>RegExp($.escapeSelector(qry),'i').test(c)))
  }})
  .on('typeahead:select typeahead:autocomplete', function(ev, suggestion) {
    var trigger = $(this);
    $('#cardmodal').on('hidden.bs.modal',function() {
      setTimeout(function () {
        trigger.typeahead('val','').trigger('input').focus();
      },100);
    });
    $('#dt').find('[data-filter="' + suggestion + '"]').find('a').trigger('click');
  }); 
      
      
$('#cardmodal')
  .on('show.bs.modal', function (ev) {
    var code = $(ev.relatedTarget).data('code');
    var crd = _db_cards.filter(c=>c.code==code)[0];
    var $title = $(this).find('.modal-title');
    var $body = $(this).find('.modal-body');
    
    var $cardrow = $('<div class="row" />');
    var $btnrow = $('<div class="d-flex justify-content-around"></div>');
    
    $body.empty();
    $cardrow.append('<div class="col-6">' + card_div(crd) + '</div>');
    $cardrow.append('<div class="col-6"><img class="img-fluid" src="' + getcgdbImageUrl(crd) + '" /></div>');
    $body.append($cardrow);
    
    $title.empty();
    $title.append('<div>' + crd.name + '</div>');
    $btnrow.append(btngroup(crd,0));
    $btnrow.append(btngroup(crd,1));
    $title.append($btnrow);
  })
  .on('change','.btn-group',function() {
    var id = $(this).data('deck');
    var code = $(this).data('code');
    var qty = $(this).find('input:checked').val();
    
    updateDecklist (id, code, qty);
  });
  
function updateDecklist (id, code, qty) {
  var decklist = JSON.parse($('#deckdata'+id).val());
  delete decklist[code];
  if (qty > 0) {
    decklist[code] = parseInt(qty,10);
  }
  $('#deckdata'+id).val(JSON.stringify(decklist))
  
  write_decklist(id);
  set_table_buttons();
  $('#savefellowship').attr('disabled',false);
  $('#savedeck' + id).attr('disabled',false);
}

function maxindeck(crd) { return (crd.type_code == "hero" ? 1 : 3); }

function btngroup(crd,id) {
  var deck = JSON.parse($('#deckdata'+id).val());
  var qty = (deck[crd.code] || 0);
  var btn;
  var $group = $('<div class="btn-group btn-group-sm btn-group-toggle mt-1" data-toggle="buttons" data-code="' + crd.code + '" data-deck='+id+'></div>');
  for (i=0;i<=maxindeck(crd);i++) {
    $btn = $('<label class="btn btn-outline-secondary" data-dismiss="modal"><input type="radio" name=' + id + ' value=' + i + '></input>'+i+'</label>');
    if (i == qty) {
      $($btn).addClass('active');
      $($btn).find('input').attr('checked',true);
    }
    $group.append($btn);
  }
  return $('<div><span class="mr-2">Deck '+(id+1)+'</span></div>').append($group);
}
  

function card_div (crd) {
  return '<div>' 
      + '<div class="d-flex"><b class="mx-auto">' + crd.traits + '</b></div>'
      + '<div class="mb-1" style="white-space: pre-wrap;">' + crd.text + '</div>'
      + '<small class="text-muted" style="white-space: pre-wrap;">' + crd.flavor + '</small>'
      + '<div class="d-flex"><small class="text-muted ml-auto">' + crd.pack_name + ' #' + crd.position + '</small></div>'
    + '</div>';
}

// decklist

$('#dt').on('click','button',function () {
  var id = $(this).closest('td').data('deck');
  var code = $(this).closest('td').data('code');
  var qty = parseInt($(this).attr('value'));
  
  updateDecklist(id,code,qty);
});


// PACKS TAB

$('#packlist')
  .on('change','input[data-type="pack"]',function () {
    var packsowned = $('#packlist').find('input:checked[data-type="pack"]').toArray().map(c=>$(c).data('code'));
    if (packsowned.length > 0) {packsowned.push("Core");}
    localStorage.setItem('lotrpacks_owned',JSON.stringify(packsowned));
    setparentcheckbox($(this));
    DT.column(10).search(packsowned.join('|'),true).draw();
  })
  .on('change','input[data-type="cycle"]',function () {
    $(this).closest('li').find('input[data-type="pack"]').prop('checked',this.checked);
    var packsowned = $('#packlist').find('input:checked[data-type="pack"]').toArray().map(c=>$(c).data('code'));
    if (packsowned.length > 0) {packsowned.push("Core");}
    localStorage.setItem('lotrpacks_owned',JSON.stringify(packsowned));
    DT.column(10).search(packsowned.join('|'),true).draw();
  });;
$('#coresets').on('change','input',function() {
  localStorage.setItem('lotrcore_owned',parseInt($(this).val()));
});
