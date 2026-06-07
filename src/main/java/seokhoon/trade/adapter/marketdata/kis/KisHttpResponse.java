package seokhoon.trade.adapter.marketdata.kis;

import tools.jackson.databind.JsonNode;

record KisHttpResponse(int statusCode, JsonNode body) {
}
