response.setHeader("X-Test","test er test");

if (request.params.fnr === '123') {
    response.setStatus(200);
} else {
    response.setStatus(666);
}

response.setResponseJson(request.params);
