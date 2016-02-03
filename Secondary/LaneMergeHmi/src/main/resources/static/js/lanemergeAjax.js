/**
 * Created with IntelliJ IDEA.
 * User: ferenced
 * Date: 8/4/15
 * Time: 6:50 PM
 * To change this template use File | Settings | File Templates.
 */


function _lanemergeAjax() {}

var lanemergeAjax = new _lanemergeAjax();

_lanemergeAjax.prototype.vehicleRole = 1;

/**
 * setParameters ajax
 * Grabs the vehicle setup commands from the user.
 */
_lanemergeAjax.prototype.ajaxSetParameters = function (vehicleRole, operatingSpeed) {
    "use strict";

    var request = $.ajax({
        url : "setParameters",
        dataType : "json",
        type : "post",
        data : {
            vehicleRole : vehicleRole,
            operatingSpeed : operatingSpeed
        }
    });

    request.done(function(response, textStatus, jqXHR) {
    });

    request.fail(function(jqXHR, textStatus, errorThrown) {
        var prefix = "An error occurred setting lanemerge parameters: ";
        var statusMessage = prefix.concat(textStatus);
        //alert(statusMessage);
    });

}

/**
 * logUiEvent ajax
 * passes an event description string to the server for logging.
 */
_lanemergeAjax.prototype.ajaxLogEvent = function(eventDescrip) {
    "use strict";

    var request = $.ajax({
        url : "logUiEvent",
        dataType : "json",
        type : "post",
        data : { eventDescrip : eventDescrip }
    });

    request.done(function(response, textStatus, jqXHR) {
    });

    request.fail(function(jqXHR, textStatus, errorThrown) {
        var prefix = "Error sending UI event logging info: ";
        var statusMessage = prefix.concat(textStatus);
    });
}