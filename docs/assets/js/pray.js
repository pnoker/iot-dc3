function checktime(time1, time2) {
    var time = Date.parse(new Date())
    var date1 = Date.parse(new Date(time1.replace(/-/g, '/')))
    var date2 = Date.parse(new Date(time2.replace(/-/g, '/')))
    if (date1 < time && date2 > time) {
        var prayDom = document.getElementsByTagName('head')[0]
        prayDom.innerHTML = "<link rel='stylesheet' href='/assets/css/pray.css'>"
    } else {
        return
    }
}

checktime('2020-04-03 00:00:00', '2020-04-04 23:59:59')
