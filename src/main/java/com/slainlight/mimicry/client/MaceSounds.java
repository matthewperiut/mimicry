package com.slainlight.mimicry.client;

//? if <1.21 {
/*import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;

// 1.20.1 has no mace sounds, so the client fetches them from Mojang's asset server by hash, as the launcher does,
// into a small built-in pack; without them it falls back to similar vanilla sounds
public final class MaceSounds {
	private static final Map<String, String> FILES = new TreeMap<>(Map.of(
		"smash_ground1", "88efd3b53d45f8cc6ae2ab39a414aa20d9760c46",
		"smash_ground2", "df4382620752d04c27e3f33be08bacc2fc89078c",
		"smash_ground3", "d761b39e2ae68753d68a3611c3d8efbf213e669a",
		"smash_ground4", "42409a52c41c8dee98159236a4fd98813f29bef6",
		"smash_ground_heavy", "6ffcfff22efc6ba726ed80d7891b1c6e707bed60"));

	private MaceSounds() {
	}

	public static RepositorySource source(Path gameDirectory) {
		Path root = gameDirectory.resolve("mimicry").resolve("mace_sounds");
		try {
			Path sounds = root.resolve("assets/mimicry/sounds/mace");
			Files.createDirectories(sounds);
			boolean complete = download(sounds);
			Files.writeString(root.resolve("pack.mcmeta"), "{\"pack\":{\"pack_format\":15,\"description\":\"Mace sounds for Mimicry\"}}");
			Files.writeString(root.resolve("assets/mimicry/sounds.json"), soundsJson(complete).toString());
		} catch (IOException e) {
			return consumer -> {
			};
		}
		return consumer -> {
			Pack pack = Pack.readMetaAndCreate("mimicry_mace_sounds", Component.literal("Mimicry mace sounds"), true,
				id -> new PathPackResources(id, root, true), PackType.CLIENT_RESOURCES, Pack.Position.TOP, PackSource.BUILT_IN);
			if (pack != null) {
				consumer.accept(pack);
			}
		};
	}

	private static boolean download(Path dir) {
		HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NORMAL).build();
		boolean complete = true;
		for (Map.Entry<String, String> file : FILES.entrySet()) {
			Path path = dir.resolve(file.getKey() + ".ogg");
			String hash = file.getValue();
			try {
				if (Files.exists(path) && sha1(Files.readAllBytes(path)).equals(hash)) {
					continue;
				}
				HttpRequest request = HttpRequest.newBuilder(URI.create("https://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash))
					.timeout(Duration.ofSeconds(10)).build();
				byte[] body = http.send(request, HttpResponse.BodyHandlers.ofByteArray()).body();
				if (!sha1(body).equals(hash)) {
					complete = false;
					continue;
				}
				Files.write(path, body);
			} catch (IOException e) {
				complete = false;
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return false;
			}
		}
		return complete;
	}

	private static String sha1(byte[] data) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(data));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private static JsonObject soundsJson(boolean complete) {
		JsonObject json = new JsonObject();
		json.add("item.mace.smash_ground", event(complete, "minecraft:entity.generic.big_fall",
			"smash_ground1", "smash_ground2", "smash_ground3", "smash_ground4"));
		json.add("item.mace.smash_ground_heavy", event(complete, "minecraft:block.anvil.land", "smash_ground_heavy"));
		return json;
	}

	private static JsonObject event(boolean complete, String fallback, String... files) {
		JsonArray sounds = new JsonArray();
		if (complete) {
			for (String file : files) {
				sounds.add("mimicry:mace/" + file);
			}
		} else {
			JsonObject event = new JsonObject();
			event.addProperty("name", fallback);
			event.addProperty("type", "event");
			sounds.add(event);
		}
		JsonObject out = new JsonObject();
		out.add("sounds", sounds);
		return out;
	}
}
*///?}
