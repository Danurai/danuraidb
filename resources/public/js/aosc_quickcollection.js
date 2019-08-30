var _filter = {}
var _freeFilter = {}
var _cards
var _collection

_collection = $('#collection').val();
_collection = (_collection != "" ? JSON.parse(_collection) : {});
$.getJSON('/aosc/api/data/cards', function(data) {
  var data_source = data['hits']['hits'].map(c => c['_source']);
  _cards = TAFFY (
    data_source
      .map(function(c) {return $.extend({"digital":0,"physical":0,"foil":0},this[c.id],c)}, _collection)
      .map(function(c) {return $.extend({"catid":this.indexOf(c.category.en),"setnumber":c.set[0].number,"text":c.effect.en},c)},["Champion","Blessing","Unit","Spell","Ability"])
    //data_source.map(function(c) {return $.extend({"digital":"0","physical":"0","foil":"0"},this[c.id],c)}, _collection).map(function (c) {return $.extend({"catid":this.indexOf(c.category.en),"setnumber":c.set[0].number},c)},["Champion","Blessing","Unit","Spell","Ability"])
  )
  _filter.alliance =  $('#alliance').find('input:checked').first().attr('id');
  _filter.category =  {"en":$('#category').find('input:checked').first().attr('id')};
  write_cards();
});

function write_stats () {
   var outp = '';
   $.each(_cards().order("setnumber").distinct("setnumber"),function (k, v) {
     outp += '<span class="mr-2">' 
       + '<b>' + _cards({"setnumber":v}).first().set[0].name + ": </b>" 
       + _cards({"setnumber":v,"digital":{">":0}}).count()
       + "/" + _cards({"setnumber":v}).count()
       + '</span>';
   });
   $('#stats').html(outp);
}

function imageName(c) {
  var sku = c.skus.filter(sku => (sku.default == true && sku.lang == "en"))[0]
  return "/img/aosc/cards/" + sku.id + ".jpg"
}

function write_cards() {
  var outp = '<table class="mx-auto"><tbody>';
  var c;
  var total = 0;
  var res = _cards($.extend({},_filter, _freeFilter)).order("catid asec, name asec").get();
  
  outp += '<tr>'
  for (i=0; i<res.length; i++) {
    if (((i % 7) == 0) && (i != 0)) {
       outp += '</tr><tr>';
    }
    total = (parseInt(res[i].digital) + parseInt(res[i].physical) + parseInt(res[i].foil));
    outp += '<td><div class="cardcontainer" data-id=' + res[i].id + '>'
      + '<img class="cardimg' + (total == 0 ? ' cardimggrey' : '') + '" src="' + imageName(res[i]) + '" alt="' + res[i].name + '">'
      //+ (total == 0 ? '<span class="greyed"></span>' : '')
      + '<span class="countbox"><span data-id=' + res[i].id + ' data-toggle="modal" data-target="#updatemodal">'
      + (total == 0 ? '<i class="fa fa-lock">' : 'x' + total)
      + '</span></span>'
      + '</div></td>';
  }
  outp+='</tr>';
  $('#cards').html (outp + '</tbody></table>');
  write_stats();
}

function valueUpdateInputs(crd) {
  return '<div class="row" data-id = ' + crd.id + '>'
      + '<div class="col-sm-4">'
      + '<label>Digital</label>'
      + '<input type="number" data-type="digital" class="form-control" value=' + crd.digital + '></input>'
      + '</div>'
      + '<div class="col-sm-4">'
      + '<label>Physical</label>'
      + '<input type="number" data-type="physical" class="form-control" value=' + crd.physical + '></input>'
      + '</div>'
      + '<div class="col-sm-4">'
      + '<label>Foil</label>'
      + '<input type="number" data-type="foil" class="form-control" value=' + crd.foil + '></input>'
      + '</div>'
    + '</div>';
}

$('#updatemodal').on('show.bs.modal', function (evt) {
  var id = $(evt.relatedTarget).closest('div').data('id');
  var crd = _cards({"id":id}).first();
  $(this).find('.modal-title').html(crd.name);
  $(this).find('.modal-body').html(valueUpdateInputs (crd) 
      + '<img class="mt-2" src="' + imageName(crd) + '"></img>');
  $($(this).find('.modal-footer a')[0]).attr("href","/aosc/cards/" + crd.id);
});

$('#updatemodal').on('change','input[type=number]',function () {
  var id = $(this).closest('.row').data('id');
  var type = $(this).data("type");
  var val = parseInt($(this).val());
  
  update_card_collection (id, type, val);
});

// Double-click to quick add 1x Digital

$(document).on('dblclick','.cardcontainer', function() {
  var id = $(this).data('id');
  var type = 'digital';
  var val = 1 + parseInt(typeof _collection[id] === 'undefined' ? 0 : _collection[id]['digital'])
  update_card_collection (id, type, val);
});

function update_card_collection (id, type, val) {
  if (typeof _collection[id] === 'undefined') {_collection[id] = {};}
  _collection[id][type] = val;
  _cards({"id":id}).update(type,val);
  var crd = _cards({"id":id}).first();
  var total = "x" + (parseInt(crd.digital) + parseInt(crd.physical) + parseInt(crd.foil));
  $('#collection').val(JSON.stringify(_collection));
  write_stats();
  
  var card_container = $('#cards').find('.cardcontainer[data-id=' + id + ']');
  if (total == "x0") {
    //if (card_container.find('span.greyed').length == 0) {
    //  card_container.append('<span class="greyed"></span>');
    //}
    card_container.find('img').addClass('cardimggrey');
    card_container.find('span.countbox').find('span[data-id=' + id + ']').html('<i class="fa fa-lock"></i>');
  } else {
    //card_container.find('span.greyed').remove();
    card_container.find('img').removeClass('cardimggrey');
    card_container.find('span.countbox').find('span[data-id=' + id + ']').html(total);
  }
  $('#btnsave').prop("disabled",false);
}
  
  
// FILTERS
$('.btn-group-toggle-none label.btn').on('click',function() {
  
  var toggleon = $(this).find('input:checked').length == 0;
  var f = $(this).closest('div')[0].id;
  
  var grp = $(this).closest('div')[0];
  $(grp).find('label').removeClass('active');
  $(grp).find('input').prop('checked',false);
  
  delete _filter[f]
  if (toggleon) {
    $(this).addClass('active');
    $(this).find('input').prop('checked',true);
    var v = $(this).find('input')[0].id;
    if (f=="category") { _filter[f] = {"en":v} }
    else { _filter[f] = v; }
  }
  
  write_cards();
  return false; // don't fire twice!
});

$('#filter').on('input',function() {
    _freeFilter = parsefilter($(this).val());
    write_cards();
  });

// IMPORT //
$('#inputcollection').on('input',function() {
  $('#importcollection').val($(this).val())
});

$('#copycollection').on('click',function() {
  $temp=$('<input>');
  $('body').append($temp);
  $temp.val($('#collection').val()).select();
  document.execCommand("copy");
  $temp.remove();
  add_toast("Collection copied to clipboard");
  $(this).select();
});