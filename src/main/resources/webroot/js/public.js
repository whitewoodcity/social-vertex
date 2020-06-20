$(document).scroll(function(){
    var sctop=$(this).scrollTop();
    if(sctop > 35){
        $(".header").addClass("sha");
    }else{
        $(".header").removeClass("sha");
    }
});
