$('a.list-group-item').on('mouseover', function () {
  var outp = '';
  var r = _cards({"code":$(this).data("code").toString()}).first();
  var sig = _cards({"signature_squad":r.signature_squad,"type_code":{"!is":"warlord_unit"}});
  
  $('#warlord').html('<img class="img-fluid rounded mx-auto d-block" src="' + r.img + '" alt="' + r.name + '"></img>');
        
  outp += '<div class="row">';
  sig.each(function (s) {
    outp += '<div class="col-sm-6">'
          + '<img class="my-2 img-fluid" src="' + s.img + '" alt="' + s.name +'"></img>'
          + '</div>';
  });
  outp += '</div>';
  $('#signaturesquad').html(outp);
  
});