var _cards;
var resultdata = document.createElement('div');
var tbody = document.createElement('tbody');
var _filter = {"setnumber":4}

$('#results').append(resultdata);
$(resultdata).html('results pending...');
create_table();

$('#filter').val(JSON.stringify(_filter));

$('#filter').on('change',function() {
  _filter = JSON.parse($(this).val());
  write_table()
});

$('#copydata').on('click', function () {
  $('#aoscdata').select();
  document.execCommand("copy");
  document.getSelection().removeAllRanges();
});

$.getJSON("/aosc/api/data/cards", function (data) {
  $('#datasrcinfo').html('<b>Source:&nbsp;</b>' +  data.source);
  delete data.source;
  $('#aoscdata').val(JSON.stringify(data));
  _cards=TAFFY(data.hits.hits.map(c => c._source).map(card => $.extend({"setnumber": card.set[0].number},card)));
  write_table();
});

function create_table() {
  var tbl = document.createElement('table');
  var thead = document.createElement('thead');
  $(tbl).addClass("table table-sm");
  $(thead).html('<tr><th>id</th><th>Name</th><th>default</th><th>finish</th><th>filename</th><th>img</th><th>img</th></tr>');
  tbl.append(thead);
  tbl.append(tbody);
  $('#results').append(tbl);
}

function write_table() {
  var ccount = 0
  var mcount = 0;
  var x;
  $(tbody).empty();
  _cards(_filter).each(function (c) {
    ccount++;
    x=0;
    $.each(c.skus,function (id, sku) {
      if (sku.finish == "matte" && sku.default == true && sku.lang == "en") {
        x++;
        $(tbody).append(write_row(c, sku));
      }
    });
    if (x>1) {mcount++}
  })
  $(resultdata).html('Cards: ' + ccount + ' with Multiples: ' + mcount);
}

function write_row(c, sku) {
  return '<tr>'
    + '<td>' + c.id + '</td>'
    + '<td>' + c.name + '</td>'
    + '<td>' + sku.default + '</td>'
    + '<td>' + sku.finish + '</td>'
    + '<td>' + sku.id + '.jpg</td>'
    + '<td><img width="50px" src="/img/aosc/cards/' + sku.id + '.jpg"></img></td>'
    + '<td><img width="50px" src="https://assets.warhammerchampions.com/card-database/cards/' + sku.id + '.jpg"></img></td>'
    + '</tr>';
}
    
  