if (request.params.fnr === '123') {
    response.setStatus(200);
} else {
    response.setStatus(500);
}

response.setResponseJson(request.params);
