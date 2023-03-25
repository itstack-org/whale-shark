package com.bantanger.im.service.strategy.command.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.bantanger.im.codec.pack.LoginPack;
import com.bantanger.im.codec.proto.Message;
import com.bantanger.im.common.comstant.Constants;
import com.bantanger.im.common.enums.connect.ImSystemConnectState;
import com.bantanger.im.common.model.UserClientDto;
import com.bantanger.im.common.model.UserSession;
import com.bantanger.im.service.redis.RedisManager;
import com.bantanger.im.service.strategy.command.BaseCommandStrategy;
import com.bantanger.im.service.utils.SessionSocketHolder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/25 9:52
 */
public class LoginCommand extends BaseCommandStrategy {

    @Override
    public void doStrategy(ChannelHandlerContext ctx, Message msg) {
        // 解析 msg
        LoginPack loginPack = JSON.parseObject(JSONObject.toJSONString(msg.getMessagePack()),
                new TypeReference<LoginPack>() {

                }.getType());
        UserClientDto userClientDto = new UserClientDto();
        userClientDto.setUserId(loginPack.getUserId());
        userClientDto.setAppId(msg.getMessageHeader().getAppId());
        userClientDto.setClientType(msg.getMessageHeader().getClientType());

        // channel 设置属性
        ctx.channel().attr(AttributeKey.valueOf(Constants.UserClientConstants.UserId)).set(userClientDto.getUserId());
        ctx.channel().attr(AttributeKey.valueOf(Constants.UserClientConstants.AppId)).set(userClientDto.getAppId());
        ctx.channel().attr(AttributeKey.valueOf(Constants.UserClientConstants.ClientType)).set(userClientDto.getClientType());

        // 将 channel 存起来
        SessionSocketHolder.put(userClientDto, (NioSocketChannel) ctx.channel());

        // Redisson 高速存储用户 Session
        UserSession userSession = new UserSession();
        userSession.setUserId(loginPack.getUserId());
        userSession.setAppId(msg.getMessageHeader().getAppId());
        userSession.setClientType(msg.getMessageHeader().getClientType());
        userSession.setConnectState(ImSystemConnectState.CONNECT_STATE_OFFLINE.getCode());
        // 存储到 Redis
        RedissonClient redissonClient = RedisManager.getRedissonClient();
        RMap<String, String> map = redissonClient.getMap(
                msg.getMessageHeader().getAppId() +
                        Constants.RedisConstants.UserSessionConstants +
                        loginPack.getUserId());
        map.put(msg.getMessageHeader().getClientType() + "",
                JSONObject.toJSONString(userSession));
    }

}