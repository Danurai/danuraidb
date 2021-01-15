var DT;
var _digicards;
var _decklist = {};

function initDataTable () {
  DT = $('#dt').DataTable({
    //pageLength: false,
    //lengthChange: false,
    paging: false,
    dom: 'tp<"small"i>',
    columnDefs: [
      {orderable: false, targets: [1]},
      {type: "num", targets: [5,6,7,8,9]} ]
  });
}

initDataTable()

// FILTERS

$('#type_code').on('change',function() {
  let flt = $('#type_code').find('input:checked').toArray().map(c=>c.id).join("|");
  DT.columns(3).search(flt,true,false).draw();
});

$('#sphere_code').on('change',function() {
  let flt = $('#sphere_code').find('input:checked').toArray().map(c=>c.id).join("|");
  DT.columns(4).search(flt,true,false).draw();
});

$('#filtertext')
  .typeahead({
    hint: true, highlight: true, minLength: 2
  },{
    name: 'lotrcardnames',
    source: 
      function findMatches (q, cb) {
        let regexp = new RegExp(q,'i');
        cb(_digicards.filter(c=>regexp.test(c.name)).map(c=>c.name))
      }
  })
  .on('typeahead:select typeahead:autocomplete input',function () {
    val = $(this).val();
    DT.columns(2).search(val).draw();
  })
  .on('input', function () {
    val = $(this).val();
    // parse traits, text
    DT.columns(2).search(val).draw();
  });

// TABLE UPDATES

function set_table_buttons () {
  DT.cells('[data-type="qty"]').nodes().to$().find('button').removeClass('active');
  DT.cells('[data-type="qty"]').nodes().to$().find('button[value=0]').addClass('active');
  
  if ($('#deckdata').val() != "") {
    $.each(JSON.parse($('#deckdata').val()), function (k, v) {
      DT.cells('[data-code="' + k + '"]').nodes().to$().find('button[value=0]').removeClass('active');
      DT.cells('[data-code="' + k + '"]').nodes().to$().find('button[value="' + v + '"]').addClass('active');
    });
  }
}


// DECKLIST UPDATES

function decklisthtml ( decklist, cards ) {
  let codes = Object.keys(decklist);
  let deckcards = _digicards.filter(c=> codes.indexOf(c.code) != -1).map(c=>$.extend({"qty": _decklist[c.code]},c))
  let outp = '';
  
// Deck Stats
  let numcards = deckcards.filter(c=>c.type_code != "hero").reduce((t,c)=>t+=parseInt(c.qty),0);
  let numheros = deckcards.filter(c=>c.type_code == "hero").reduce((t,c)=>t+=parseInt(c.qty),0);
  
  outp += '<div><b>Starting Threat: </b>' + deckcards.filter(c=>c.type_code == "hero").reduce((t,c)=>t+=(c.threat),0) + '</div>';
  outp += '<div>Heros: ' + (numheros > 3 ? '<span class="text-warning">' : '<span class="text-primary">') + numheros + '/3 </span><small>(max)</small>';
  outp += '<div class="mb-2">Cards: ' + (numcards < 30 ? '<span class="text-warning">' : '<span class="text-primary">') + numcards + '/30</span>';
  
// Heroes
  outp += '<div class="d-flex mb-2">';
  $.each(deckcards.filter(c=>c.type_code == "hero"),function (i, c) {
    outp += '<a class="card-link-digital mr-2" '
      + 'href="lotrdb/card/' + c.code + '" '
      + 'data-code="' + c.code + '" data-toggle="modal" data-target="#cardmodal">'
      + '<img class="img-fluid deckcard" src="/img/lotrdb/cards/' + c.code + '.png" />'
      + '</a>';
  });
  outp += '</div>';
  
// Decklist
  outp += '<div style="-webkit-column-gap: 20px; -webkit-column-count: 2; -moz-column-gap: 20px; -moz-column-count: 2; column-gap: 20px; column-count: 2;">';
  $.each(["Ally","Attachment","Event"],function (n, t) {
    var cardsOfType = deckcards.filter(c=>c.type_name == t);
    if (cardsOfType.length > 0) {
      outp += '<div class="mb-2" style="break-inside: avoid;"><span class="font-weight-bold">' + t + ' (' + cardsOfType.reduce((t,c)=>t+=parseInt(c.qty),0) + ')</span>';
      $.each(deckcards.filter(c=>c.type_name == t), function (i, c) {
        outp += '<div>' + c.qty + 'x '
          + '<a class="card-link-digital" data-toggle="modal" data-target="#cardmodal" data-code="' + c.code + '" href="/lotrdb/card/' + c.code + '">' 
          + c.name 
          //+ (_db_cards({"name":c.name,"pack_code":_filter.pack_code}).count()>1 ? ' <small>(' + c.pack_code +')</small>' : '')
          + '<span class="lotr-type-'+c.sphere_code+' ml-1 fa-sm"></a></div>';
      });
      outp+='</div>';
    }
  });
  outp+='</div>';
  return outp;
}


function write_deck () {
  $('#deckdata').val(JSON.stringify(_decklist))
  $('#decklist').html(decklisthtml(_decklist, _digicards))
  
  set_table_buttons()
}




$('#dt').on('click','button',function () {
  let code = $(this).closest('td').data('code')
  let qty = parseInt(this.value);
  if (qty == 0) {
    delete _decklist[code];
  } else {
    _decklist[code] = qty;
  }
  write_deck()
});

  
$.getJSON("/lotrdb/api/data/cardsdigital",function(data) {
  _digicards = data;
  let dl = $('#deckdata').val();
  if (dl != "") {_decklist = JSON.parse(dl);}
  write_deck()
});

//Modal


function modalButtonGroup(crd) {
  var count = (_decklist[crd.code] || 0);
  var outp =  '<div class="btn-group btn-group-toggle" data-toggle="buttons">';
  for (i=0; i<=crd.deck_limit; i++) {
    outp += '<label class="btn'
      + (crd.owned < i ? ' btn-outline-danger' : ' btn-outline-secondary')
      + (count == i ? ' active' : '')
      + '">'
      + '<input type="radio" value="' + i + '" name="' + crd.id + '" data-code="' + crd.code + '">'
      + i
      + '</label>';
  }
  outp += '</div>';
  return outp;
}

function setModalHtml(modal,crd) {
  modal
    .find('.modal-header')
    .html('<div class="modal-title"><h4 class="mr-2 align-middle">' 
      + ($.inArray("Unique",crd.tags) != -1 ? '&bull;&nbsp;' : '')
      + crd.name 
      + '</h4>'
      + modalButtonGroup(crd)
      + '</div>'
      + '<button class="close" type="button" data-dismiss="modal"><span>&times;</span></button>');
  modal
    .find('.modal-body')
    .html('<img src="/img/lotrdb/cards/' + crd.code + '.png" class="img-fluid" />');
  modal
    .find('.modal-footer')
    .html('<span>' + crd.pack_name + ' #' + crd.position + '</span>');
}  

$('body').on('click','.card-link-digital',function (e) {
  e.preventDefault();
});

$('#cardmodal')
  .on('show.bs.modal', function (event) {
    var code = $(event.relatedTarget).data("code").toString(); // Button that triggered the modal
    var crd = _digicards.filter(c=>c.code==code)[0];
    if (typeof crd.code !== 'undefined') {setModalHtml($(this),crd);}
  })
  .on('hidden.bs.modal', function () {
    $(this).find('.modal-header').html('');
    $(this).find('.modal-body').html('');
  })
  .on('change','input[type=radio]',function () {
    var code = $(this).data("code");
    var val = parseInt($(this).attr("value"));
    if (val == 0) {
      delete _decklist[code]
    } else {
      _decklist[code] = val
    }
    write_deck();
    $('#cardmodal').modal('hide');
  })
  .on('keypress',function (ev)  {
    var num = parseInt(ev.which, 10) - 48;
    $('.modal input[type=radio][value=' + num + ']').trigger('change');
  });;