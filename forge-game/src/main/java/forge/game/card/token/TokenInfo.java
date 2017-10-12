package forge.game.card.token;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import forge.ImageKeys;
import forge.card.CardType;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardFactoryUtil;
import forge.game.player.Player;

import java.util.List;
import java.util.Map;

public class TokenInfo {
    final String name;
    final String imageName;
    final String manaCost;
    final String[] types;
    final String[] intrinsicKeywords;
    final int basePower;
    final int baseToughness;

    public TokenInfo(String name, String imageName, String manaCost, String[] types,
                     String[] intrinsicKeywords, int basePower, int baseToughness) {
        this.name = name;
        this.imageName = imageName;
        this.manaCost = manaCost;
        this.types = types;
        this.intrinsicKeywords = intrinsicKeywords;
        this.basePower = basePower;
        this.baseToughness = baseToughness;
    }

    public TokenInfo(Card c) {
        this.name = c.getName();
        this.imageName = ImageKeys.getTokenImageName(c.getImageKey());
        this.manaCost = c.getManaCost().toString();
        this.types = getCardTypes(c);
        this.intrinsicKeywords   = c.getKeywords().toArray(new String[0]);
        this.basePower = c.getBasePower();
        this.baseToughness = c.getBaseToughness();
    }

    public TokenInfo(String str) {
        final String[] tokenInfo = str.split(",");
        int power = 0;
        int toughness = 0;
        String manaCost = "0";
        String[] types = null;
        String[] keywords = null;
        String imageName = null;
        for (String info : tokenInfo) {
            int index = info.indexOf(':');
            if (index == -1) {
                continue;
            }
            String remainder = info.substring(index + 1);
            if (info.startsWith("P:")) {
                power = Integer.parseInt(remainder);
            } else if (info.startsWith("T:")) {
                toughness = Integer.parseInt(remainder);
            } else if (info.startsWith("Cost:")) {
                manaCost = remainder;
            } else if (info.startsWith("Types:")) {
                types = remainder.split("-");
            } else if (info.startsWith("Keywords:")) {
                keywords = remainder.split("-");
            } else if (info.startsWith("Image:")) {
                imageName = remainder;
            }
        }

        this.name = tokenInfo[0];
        this.imageName = imageName;
        this.manaCost = manaCost;
        this.types = types;
        this.intrinsicKeywords = keywords;
        this.basePower = power;
        this.baseToughness = toughness;
    }

    private static String[] getCardTypes(Card c) {
        List<String> relevantTypes = Lists.newArrayList();
        for (CardType.CoreType t : c.getType().getCoreTypes()) {
            relevantTypes.add(t.name());
        }
        Iterables.addAll(relevantTypes, c.getType().getSubtypes());
        if (c.getType().isLegendary()) {
            relevantTypes.add("Legendary");
        }
        return relevantTypes.toArray(new String[relevantTypes.size()]);
    }

    private Card toCard(Game game) {
        final Card c = new Card(game.nextCardId(), game);
        c.setName(name);
        c.setImageKey(ImageKeys.getTokenKey(imageName));

        // TODO - most tokens mana cost is 0, this needs to be fixed
        // c.setManaCost(manaCost);
        c.setColor(manaCost);
        c.setToken(true);

        for (final String t : types) {
            c.addType(t);
        }

        c.setBasePower(basePower);
        c.setBaseToughness(baseToughness);
        return c;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(',');
        sb.append("P:").append(basePower).append(',');
        sb.append("T:").append(baseToughness).append(',');
        sb.append("Cost:").append(manaCost).append(',');
        sb.append("Types:").append(Joiner.on('-').join(types)).append(',');
        sb.append("Keywords:").append(Joiner.on('-').join(intrinsicKeywords)).append(',');
        sb.append("Image:").append(imageName);
        return sb.toString();
    }

    public List<Card> makeTokenWithMultiplier(final Player controller, final boolean applyMultiplier) {
        final List<Card> list = Lists.newArrayList();
        final Game game = controller.getGame();

        int multiplier = 1;

        final Map<String, Object> repParams = Maps.newHashMap();
        repParams.put("Event", "CreateToken");
        repParams.put("Affected", controller);
        repParams.put("TokenNum", multiplier);
        repParams.put("EffectOnly", applyMultiplier);

        switch (game.getReplacementHandler().run(repParams)) {
            case NotReplaced:
                break;
            case Updated: {
                multiplier = (int) repParams.get("TokenNum");
                break;
            }
            default:
                return list;
        }

        for (int i = 0; i < multiplier; i++) {
            Card temp = toCard(game);

            for (final String kw : intrinsicKeywords) {
                temp.addIntrinsicKeyword(kw);
            }
            temp.setOwner(controller);
            temp.setToken(true);
            CardFactoryUtil.parseKeywords(temp, temp.getName());
            CardFactoryUtil.setupKeywordedAbilities(temp);
            list.add(temp);
        }
        return list;
    }

    public Card makeOneToken(final Player controller) {
        final Game game = controller.getGame();
        final Card c = toCard(game);

        for (final String kw : intrinsicKeywords) {
            c.addIntrinsicKeyword(kw);
        }

        c.setOwner(controller);
        c.setToken(true);
        CardFactoryUtil.parseKeywords(c, c.getName());
        CardFactoryUtil.setupKeywordedAbilities(c);
        return c;
    }
}